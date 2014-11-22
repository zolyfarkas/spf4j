package org.spf4j.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.annotation.concurrent.ThreadSafe;

/**
 *
 * @author zoly
 */
@ThreadSafe
//CHECKSTYLE IGNORE InnerAssignment FOR NEXT 2000 LINES
public final class PipedOutputStream extends OutputStream {

    private final byte[] buffer;

    private int startIdx;
    private int endIdx;
    private boolean closed;

    public PipedOutputStream() {
        this(8192);
    }

    public PipedOutputStream(final int bufferSize) {
        buffer = new byte[bufferSize];
        startIdx = 0;
        endIdx = 0;
        closed = false;
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        int bytesWritten = 0;
        while (bytesWritten < len) {
            synchronized (buffer) {
                int availableToWrite = 0;
                while (!closed && (availableToWrite = availableToWrite()) < 1) {
                    try {
                        buffer.wait(10000);
                    } catch (InterruptedException ex) {
                        throw new IOException("Interrupted while writing " + Arrays.toString(b), ex);
                    }
                }
                if (closed) {
                    throw new IOException("Cannot write stream closed " + this);
                }
                availableToWrite = Math.min(availableToWrite, len - bytesWritten);
                int wrToEnd = Math.min(availableToWrite, buffer.length - endIdx);
                System.arraycopy(b, off + bytesWritten, buffer, endIdx, wrToEnd);
                endIdx += wrToEnd;
                bytesWritten += wrToEnd;
                int wrapArrond = availableToWrite - wrToEnd;
                if (wrapArrond > 0) {
                    System.arraycopy(b, off + bytesWritten, buffer, 0, wrapArrond);
                    endIdx = wrapArrond;
                    bytesWritten += wrapArrond;
                } else if (endIdx >= buffer.length) {
                    endIdx = 0;
                }
                buffer.notifyAll();
            }
        }
    }

    @Override
    public void write(final int b) throws IOException {
        synchronized (buffer) {
            while (!closed && availableToWrite() < 1) {
                try {
                    buffer.wait(10000);
                } catch (InterruptedException ex) {
                    throw new IOException("Interrupted while writing " + b, ex);
                }
            }
            if (closed) {
                throw new IOException("Cannot write stream closed " + this);
            }
            buffer[endIdx++] = (byte) b;
            if (endIdx >= buffer.length) {
                endIdx = 0;
            }
            buffer.notifyAll();
        }
    }

    private int availableToWrite() {
        if (startIdx <= endIdx) {
            return startIdx + buffer.length - endIdx - 1;
        } else {
            return startIdx - endIdx - 1;
        }
    }

    private int availableToRead() {
        if (startIdx <= endIdx) {
            return endIdx - startIdx;
        } else {
            return buffer.length - startIdx + endIdx;
        }
    }

    @Override
    public void close() {
        synchronized (buffer) {
            closed = true;
            buffer.notifyAll();
        }

    }

    public InputStream getInputStream() {
        return new InputStream() {

            @Override
            public int read() throws IOException {
                synchronized (buffer) {
                    int availableToRead = 0;
                    while ((availableToRead = availableToRead()) < 1 && !closed) {
                        try {
                            buffer.wait(10000);
                        } catch (InterruptedException ex) {
                            throw new IOException("Interrupted while reading from "
                                    + Arrays.toString(buffer) + " startIdx = " + startIdx + " endIdx = " + endIdx, ex);
                        }
                    }
                    if (availableToRead == 0) {
                        if (!closed) {
                            throw new IllegalStateException("Stream must be closed");
                        }
                        return -1;
                    }
                    int result = buffer[startIdx];
                    startIdx++;
                    if (startIdx >= buffer.length) {
                        startIdx = 0;
                    }
                    buffer.notifyAll();
                    return result;
                }
            }
            
            
            public int read(final byte [] b, final int off, final int len) throws IOException {
                int bytesWritten = 0;
                synchronized (buffer) {
                    int availableToRead = 0;
                    while ((availableToRead = availableToRead()) < 1 && !closed) {
                        try {
                            buffer.wait(10000);
                        } catch (InterruptedException ex) {
                            throw new IOException("Interrupted while reading from " + PipedOutputStream.this, ex);
                        }
                    }
                    if (availableToRead == 0) {
                        if (!closed) {
                            throw new IllegalStateException("Stream should  be closed, " + PipedOutputStream.this);
                        }
                        return -1;
                    }
                    availableToRead = Math.min(availableToRead, len);
                    int readToEnd = Math.min(availableToRead, buffer.length - startIdx);
                    System.arraycopy(buffer, startIdx, b, off, readToEnd);
                    bytesWritten += readToEnd;
                    startIdx += readToEnd;
                    int remaining = availableToRead - readToEnd;
                    if (remaining > 0) {
                        System.arraycopy(buffer, 0, b, off + readToEnd, remaining);
                        bytesWritten += remaining;
                        startIdx = remaining;
                    } else if (startIdx >= buffer.length) {
                        startIdx = 0;
                    }
                   
                    buffer.notifyAll();
                    return bytesWritten;
                }
            }

            
            
            @Override
            public void close() {
                synchronized (buffer) {
                    if (!closed) {
                        closed = true;
                        buffer.notifyAll();
                    }
                }
            }
 
        };

    }

    @Override
    public String toString() {
        synchronized (buffer) {
            return "PipedOutputStream{" + "buffer=" + Arrays.toString(buffer) + ", startIdx=" + startIdx
                + ", endIdx=" + endIdx + ", closed=" + closed + '}';
        }
    }
    
    

}
