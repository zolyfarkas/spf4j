 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.spf4j.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.annotation.WillClose;
import org.spf4j.recyclable.SizedRecyclingSupplier;
import org.spf4j.recyclable.impl.ArraySuppliers;

@SuppressFBWarnings("VO_VOLATILE_REFERENCE_TO_ARRAY")
@CleanupObligation
public final class BufferedInputStream extends FilterInputStream {

    private static int defaultBufferSize = 8192;

    private volatile byte[] buf;

    private static final AtomicReferenceFieldUpdater<BufferedInputStream, byte[]> BUF_UPDATER
            = AtomicReferenceFieldUpdater.newUpdater(BufferedInputStream.class, byte[].class, "buf");

    private int count;

    private int pos;

    private int markpos = -1;

    private int marklimit;

    private final SizedRecyclingSupplier<byte[]> bufferProvider;

    private InputStream getInIfOpen() throws IOException {
        InputStream input = in;
        if (input == null) {
            throw new IOException("Stream closed " + in);
        }
        return input;
    }

    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if (buffer == null) {
            throw new IOException("Stream closed " + in);
        }
        return buffer;
    }

    public BufferedInputStream(final InputStream in) {
        this(in, defaultBufferSize);
    }

    public BufferedInputStream(final InputStream in, final int size) {
        this(in, size, ArraySuppliers.Bytes.JAVA_NEW);
    }

    public BufferedInputStream(final InputStream in, final int size,
            final SizedRecyclingSupplier<byte[]> bufferProvider) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0 : " + size);
        }
        this.bufferProvider = bufferProvider;
        buf = bufferProvider.get(size);
    }

    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        if (markpos < 0) {
            pos = 0;
        } else if (pos >= buffer.length) {
            if (markpos > 0) {
                int sz = pos - markpos;
                System.arraycopy(buffer, markpos, buffer, 0, sz);
                pos = sz;
                markpos = 0;
            } else if (buffer.length >= marklimit) {
                markpos = -1;
                pos = 0;
            } else {
                int nsz = pos * 2;
                if (nsz > marklimit) {
                    nsz = marklimit;
                }
                byte[] nbuf = bufferProvider.get(nsz);
                if (!BUF_UPDATER.compareAndSet(this, buffer, nbuf)) {
                    bufferProvider.recycle(nbuf);
                    throw new IOException("Stream closed " + in);
                }
                System.arraycopy(buffer, 0, nbuf, 0, pos);
                bufferProvider.recycle(buffer);
                buffer = nbuf;
            }
        }
        count = pos;
        int n = getInIfOpen().read(buffer, pos, buffer.length - pos);
        if (n > 0) {
            count = n + pos;
        }
    }

    public synchronized int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count) {
                return -1;
            }
        }
        return getBufIfOpen()[pos++] & 0xff;
    }

    private int read1(final byte[] b, final int off, final int len) throws IOException {
        int avail = count - pos;
        if (avail <= 0) {
            if (len >= getBufIfOpen().length && markpos < 0) {
                return getInIfOpen().read(b, off, len);
            }
            fill();
            avail = count - pos;
            if (avail <= 0) {
                return -1;
            }
        }
        int cnt = (avail < len) ? avail : len;
        System.arraycopy(getBufIfOpen(), pos, b, off, cnt);
        pos += cnt;
        return cnt;
    }

    @Override
    public synchronized int read(final byte[] b, final int off, final int len)
            throws IOException {
        getBufIfOpen(); // Check for closed stream
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int n = 0;
        for (;;) {
            int nread = read1(b, off + n, len - n);
            if (nread <= 0) {
                return (n == 0) ? nread : n;
            }
            n += nread;
            if (n >= len) {
                return n;
            }
            // if not closed but no bytes available, return
            InputStream input = in;
            if (input != null && input.available() <= 0) {
                return n;
            }
        }
    }

    @Override
    public synchronized long skip(final long n) throws IOException {
        getBufIfOpen(); // Check for closed stream
        if (n <= 0) {
            return 0;
        }
        long avail = count - pos;

        if (avail <= 0) {
            // If no mark position set then don't keep in buffer
            if (markpos < 0) {
                return getInIfOpen().skip(n);
            }

            // Fill in buffer to save bytes for reset
            fill();
            avail = count - pos;
            if (avail <= 0) {
                return 0;
            }
        }

        long skipped = (avail < n) ? avail : n;
        pos += skipped;
        return skipped;
    }

    @Override
    public synchronized int available() throws IOException {
        return getInIfOpen().available() + (count - pos);
    }

    @Override
    public synchronized void mark(final int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }

    @Override
    public synchronized void reset() throws IOException {
        getBufIfOpen(); // Cause exception if closed
        if (markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        pos = markpos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    @WillClose
    public void close() throws IOException {
        byte[] buffer;
        while ((buffer = buf) != null) {
            if (BUF_UPDATER.compareAndSet(this, buffer, null)) {
                bufferProvider.recycle(buffer);
                InputStream input = in;
                in = null;
                if (input != null) {
                    input.close();
                }
                return;
            }
        }
    }
}
