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
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillClose;
import org.spf4j.base.Base64;
import org.spf4j.recyclable.SizedRecyclingSupplier;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * Better that the JDK outputstream, no exception swallowing...
 * and ability to recycle underlying byte array.
 * @author zoly
 */
@CleanupObligation
public final class BufferedOutputStream extends OutputStream {

    private byte[] buf;
    private final int length;
    private final OutputStream os;
    private final SizedRecyclingSupplier<byte[]> bufferProvider;

    private int count;

    public BufferedOutputStream(final OutputStream out) {
        this(out, 8192);
    }

    public BufferedOutputStream(final OutputStream out, final int size) {
        this(out, size, ArraySuppliers.Bytes.JAVA_NEW);
    }

    public BufferedOutputStream(final OutputStream out, final int size,
            final SizedRecyclingSupplier<byte[]> bufferProvider) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0 : " + size);
        }
        this.bufferProvider = bufferProvider;
        buf = bufferProvider.get(size);
        length = size;
        this.os = out;
    }

    private void flushBuffer() throws IOException {
        if (count > 0) {
            os.write(buf, 0, count);
            count = 0;
        }
    }

    @Override
    public synchronized void write(final int b) throws IOException {
        if (buf == null) {
            throw new IOException("Stream closed " + os);
        }
        if (count >= length) {
            flushBuffer();
        }
        buf[count++] = (byte) b;
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
        if (buf == null) {
            throw new IOException("Stream closed " + os);
        }
        if (len >= length) {
            flushBuffer();
            os.write(b, off, len);
            return;
        }
        if (len > length - count) {
            flushBuffer();
        }
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    @Override
    public synchronized void flush() throws IOException {
        if (buf == null) {
            throw new IOException("Stream closed " + os);
        }
        flushBuffer();
        os.flush();
    }

    @Override
    @WillClose
    public synchronized void close() throws IOException {
        if (buf != null) {
            try (OutputStream los = os) {
                flush();
            } finally {
                this.bufferProvider.recycle(buf);
                buf = null;
            }
        }
    }

    @Override
    public String toString() {
        return "BufferedOutputStream{" + "buf=" + (buf != null ? Base64.encodeBase64(buf) : "null")
                + ", length=" + length + ", os=" + os + ", bufferProvider="
                + bufferProvider + ", count=" + count + '}';
    }



}
