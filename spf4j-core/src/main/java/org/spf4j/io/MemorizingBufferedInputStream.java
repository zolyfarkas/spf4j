package org.spf4j.io;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Arrays;
import org.spf4j.base.Strings;
import org.spf4j.recyclable.SizedRecyclingSupplier;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * Why another buffered input stream?
 * Main use case if for troubleshooting.
 * Allows you to get more detail on where your stream processing failed.
 * @author zoly
 */
@ParametersAreNonnullByDefault
@CleanupObligation
public final class MemorizingBufferedInputStream extends FilterInputStream {

    private final byte[] memory;

    private final  SizedRecyclingSupplier<byte[]> bufferProvider;

    private final int readSize;

    private final Charset charset;

    private int memIdx;

    private int startIdx;

    private int endIdx;

    private boolean isEof;

    private boolean isClosed;

    public MemorizingBufferedInputStream(final InputStream in) {
        this(in, 16384, 8192, ArraySuppliers.Bytes.GL_SUPPLIER, Charsets.UTF_8);
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
        memory = bufferProvider.get(size);
        this.bufferProvider = bufferProvider;
        this.charset = charset;
        this.startIdx = 0;
        this.endIdx = 0;
        this.readSize = readSize;
        this.isEof = false;
        this.isClosed = false;
    }

    @Override
    public synchronized void close() throws IOException {
        isClosed = true;
        bufferProvider.recycle(memory);
        super.close();
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
        byte [] result = new byte[availableInMemory];
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
        byte [] result = new byte[availableToRead];
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
            if (memIdx + toFree >= startIdx) {
                memIdx = startIdx - 1;
                return availableToWrite();
            } else {
                memIdx += toFree;
                return size;
            }
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
        if (availableToRead == 0 && isEof) {
            return -1;
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
        if (availableToRead == 0  && isEof) {
            return -1;
        }
        int result = memory[startIdx++];
        if (startIdx >= memory.length) {
            startIdx = 0;
        }
        if (startIdx == endIdx) {
            throw new IllegalStateException("State " + this);
        }
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
    public String toString() {
        StringBuilder result = new StringBuilder((availableToRead() + availableInMemory()) * 2 + 128);

        result.append("MemorizingBufferedInputStream{\n");
        if (charset == null) {
            final BaseEncoding base64 = BaseEncoding.base64();
            result.append("readBytes=\"").append(base64.encode(getReadBytesFromBuffer())).append("\",\n");
            result.append("unreadBytes=\"").append(base64.encode(getUnreadBytesFromBuffer())).append("\"\n");
        } else {
            result.append("readStr=\"").append(
                    Strings.fromUtf8(getReadBytesFromBuffer())).append("\",\n");
            result.append("unreadStr=\"").append(
                    Strings.fromUtf8(getUnreadBytesFromBuffer())).append("\"\n");
        }
        return result.toString();
    }



}
