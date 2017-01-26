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

import com.google.common.io.BaseEncoding;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Arrays;
import org.spf4j.recyclable.SizedRecyclingSupplier;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * Why another buffered input stream?
 * Main use case if for troubleshooting.
 * Allows you to get more detail on where your stream processing failed.
 *
 * Implementation is a circular byte buffer, where you have 2 sizes to control the behavior:
 *
 * buffer size - the total sie of the buffer.
 * read size - the maximum number of read bytes kept in the buffer.
 *
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@CleanupObligation
public final class MemorizingBufferedInputStream extends FilterInputStream {

    private byte[] memory;

    private final  SizedRecyclingSupplier<byte[]> bufferProvider;

    private final int readSize;

    private final Charset charset;

    private int memIdx;

    private int startIdx;

    private int endIdx;

    private boolean isEof;

    private boolean isClosed;

    private long readBytes;

    public MemorizingBufferedInputStream(final InputStream in) {
        this(in, 16384, 8192, ArraySuppliers.Bytes.GL_SUPPLIER, null);
    }

    public MemorizingBufferedInputStream(final InputStream in, final int size) {
        this(in, size, size / 2, ArraySuppliers.Bytes.GL_SUPPLIER, null);
    }

    public MemorizingBufferedInputStream(final InputStream in, final Charset charset) {
        this(in, 16384, 8192, ArraySuppliers.Bytes.GL_SUPPLIER, charset);
    }

    public MemorizingBufferedInputStream(final InputStream in,
            final int size, final int readSize,
            final SizedRecyclingSupplier<byte[]> bufferProvider,
            @Nullable final Charset charset) {
        super(in);
        if (readSize > size) {
            throw new IllegalArgumentException("Read size " + readSize + " cannot be greater than " + size);
        }
        if (size < 2) {
            throw new IllegalArgumentException("Buffer size " + size + " cannot be smaler than 2");
        }
        memory = bufferProvider.get(size);
        this.bufferProvider = bufferProvider;
        this.charset = charset;
        this.startIdx = 0;
        this.endIdx = 0;
        this.readSize = readSize;
        this.isEof = false;
        this.isClosed = false;
        this.readBytes = 0;
    }

    @Override
    @DischargesObligation
    public synchronized void close() throws IOException {
        if (!isClosed) {
            isClosed = true;
            bufferProvider.recycle(memory);
            memory = null;
            super.close();
        }
    }

    private int availableToWrite() {
        if (memIdx <= endIdx) {
            return memIdx + memory.length - endIdx - 1;
        } else {
            return memIdx - endIdx - 1;
        }
    }

    private int availableToRead() {
        if (startIdx <= endIdx) {
            return endIdx - startIdx;
        } else {
            return memory.length - startIdx + endIdx;
        }
    }

    private int availableInMemory() {
        if (memIdx <= startIdx) {
            return startIdx - memIdx;
        } else {
            return memory.length - memIdx + startIdx;
        }
    }


    public synchronized byte[] getReadBytesFromBuffer() {
        final int availableInMemory = availableInMemory();
        if (availableInMemory == 0) {
            return Arrays.EMPTY_BYTE_ARRAY;
        }
        byte[] result = new byte[availableInMemory];
        if (memIdx < startIdx) {
            System.arraycopy(memory, memIdx, result, 0, result.length);
        } else {
            final int toEnd = memory.length - memIdx;
            System.arraycopy(memory, memIdx, result, 0, toEnd);
            System.arraycopy(memory, 0, result, toEnd, startIdx);
        }
        return result;
    }

    public synchronized byte[] getUnreadBytesFromBuffer() {
        final int availableToRead = availableToRead();
        if (availableToRead == 0) {
            return Arrays.EMPTY_BYTE_ARRAY;
        }
        byte[] result = new byte[availableToRead];
        if (startIdx < endIdx) {
            System.arraycopy(memory, startIdx, result, 0, result.length);
        } else {
            final int toEnd = memory.length - startIdx;
            System.arraycopy(memory, startIdx, result, 0, toEnd);
            System.arraycopy(memory, 0, result, toEnd, endIdx);
        }
        return result;
    }


    private int tryCleanup(final int size) {
        int canWrite = availableToWrite();
        if (canWrite < size) {
            int toFree = size - canWrite;
            int availableToFree = availableInMemory();
            if (toFree > availableToFree) {
                toFree = availableToFree;
            }
            memIdx = memIdx + toFree;
            if (memIdx >= memory.length) {
                memIdx = memIdx - memory.length;
            }
            return toFree + canWrite;
        } else {
            return size;
        }
    }

    private void fill() throws IOException {
        int size = tryCleanup(readSize);
        if (size < readSize) {
            throw new IllegalStateException("Illegal state " + this);
        }
        int canWriteInBulk = Math.min(size, memory.length - endIdx);
        int read = super.read(memory, endIdx, canWriteInBulk);
        if (read < 0) {
            isEof = true;
            return;
        }
        endIdx += read;
        if (read < canWriteInBulk) {
            return;
        }
        int wrapArround = size - canWriteInBulk;
        if (wrapArround > 0) {
            read = super.read(memory, 0, wrapArround);
            if (read < 0) {
              if (endIdx >= memory.length) {
                endIdx = 0;
              }
              isEof = true;
              return;
            }
            endIdx = read;
        } else if (endIdx >= memory.length) {
            endIdx = 0;
        }
    }

    @Override
    public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
        if (isClosed) {
            throw new IOException("Stream is closed " + this);
        }
        int availableToRead = availableToRead();
        if (availableToRead <= 0) {
            fill();
            availableToRead = availableToRead();
        }
        if (availableToRead == 0) {
            if (isEof) {
                return -1;
            } else {
                throw new IllegalStateException("State=" + this);
            }
        }
        int toRead = Math.min(availableToRead, len);
        int readToEnd = Math.min(toRead, memory.length - startIdx);
        System.arraycopy(memory, startIdx, b, off, readToEnd);
        startIdx += readToEnd;
        int wrapArround = toRead - readToEnd;
        if (wrapArround > 0) {
            System.arraycopy(memory, 0, b, off + readToEnd, wrapArround);
            startIdx = wrapArround;
        } else if (startIdx >= memory.length) {
            startIdx = 0;
        }
        this.readBytes += toRead;
        return toRead;
    }


    @Override
    public synchronized int read() throws IOException {
        if (isClosed) {
            throw new IOException("Stream is closed " + this);
        }
        int availableToRead = availableToRead();
        if (availableToRead <= 0) {
            fill();
            availableToRead = availableToRead();
        }
        if (availableToRead == 0) {
            if (isEof) {
                return -1;
            } else {
                throw new IllegalStateException("State=" + this);
            }
        }
        int result = memory[startIdx++] & 0xff;
        if (startIdx >= memory.length) {
            startIdx = 0;
        }
        this.readBytes++;
        return result;
    }

    @Override
    public synchronized int available() throws IOException {
        if (isClosed) {
            throw new IOException("Stream is closed " + this);
        }
        return availableToRead();
    }

    @Override
    public synchronized String toString() {
        StringBuilder result;
        if (isClosed) {
           result = new StringBuilder(64);
        } else {
           result = new StringBuilder((availableToRead() + availableInMemory()) * 2 + 128);
        }
        result.append("MemorizingBufferedInputStream{\n");
        if (isClosed) {
            result.append("closed=true\n");
        } else if (charset == null) {
            final BaseEncoding base64 = BaseEncoding.base64();
            result.append("readBytes=\"").append(base64.encode(getReadBytesFromBuffer())).append("\",\n");
            result.append("unreadBytes=\"").append(base64.encode(getUnreadBytesFromBuffer())).append("\"\n");
        } else {
          try {
            result.append("readStr=\"").append(
                    charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
                            .onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?")
                            .decode(ByteBuffer.wrap(getReadBytesFromBuffer()))).append("\",\n");
            result.append("unreadStr=\"").append(
                    charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
                            .onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?")
                            .decode(ByteBuffer.wrap(getUnreadBytesFromBuffer()))).append("\"\n");
          } catch (CharacterCodingException ex) {
            throw new RuntimeException(ex);
          }

        }
        result.append("memIdx=").append(this.memIdx).append("\"\n");
        result.append("startIdx=").append(this.startIdx).append("\"\n");
        result.append("endIdx=").append(this.endIdx).append("\"\n");
        result.append('}');
        return result.toString();
    }

    public synchronized long getReadBytes() {
        return readBytes;
    }

    @Override
    public synchronized long skip(final long n) throws IOException {
        long nrSkipped = 0;
        for (int i = 0; i < n; i++) {
            int read = read();
            if (read < 0) {
                break;
            } else {
                nrSkipped++;
            }
        }
        return nrSkipped;
    }




}
