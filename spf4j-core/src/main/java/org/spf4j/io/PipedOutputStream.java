/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.io;

import com.google.common.io.BaseEncoding;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;
import org.spf4j.recyclable.SizedRecyclingSupplier;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * Equivalent to Java piped input/output stream.
 *
 * This implementation supports timeouts, timeout are specified by setting in the ExecutionContext
 *
 * Implementation supports multiple readers and writers.
 *
 * Data is available to readers only after it is flushed. (happens automatically when buffer is full)
 *
 * This implementation should be slightly faster than the JDK implementation.
 *
 * @author zoly
 */
@ThreadSafe
@CleanupObligation
@SuppressWarnings("checkstyle:InnerAssignment")
public final class PipedOutputStream extends OutputStream {

  private byte[] buffer;

  private final Object sync = new Object();

  private int startIdx;
  private int endIdx;
  private int readerPerceivedEndIdx;
  private boolean writerClosed;
  private Exception closedException;
  private int nrReadStreams;
  private final SizedRecyclingSupplier<byte[]> bufferProvider;
  private final Long globalDeadlineNanos;

  public PipedOutputStream() {
    this(8192);
  }

  public PipedOutputStream(final int bufferSize) {
    this(bufferSize, ArraySuppliers.Bytes.JAVA_NEW);
  }

  /**
   * @deprecated use constructor that takes globalDeadlineNanos.
   */
  @Deprecated
  public PipedOutputStream(final int bufferSize, final long globalDeadlineMillis) {
    this(bufferSize, ArraySuppliers.Bytes.JAVA_NEW, globalDeadlineMillis);
  }

  /**
   * Create a PipedOutputStream with a global deadline relative to System.nanoTime().
   * @param globalDeadlineNanos deadline relative to System.nanoTime().
   * @param bufferSize the buffer size.
   */
  public PipedOutputStream(final long globalDeadlineNanos, final int bufferSize) {
    this(globalDeadlineNanos, bufferSize, ArraySuppliers.Bytes.JAVA_NEW);
  }

  public PipedOutputStream(final int bufferSize,
          final SizedRecyclingSupplier<byte[]> bufferProvider) {
    this(null, bufferSize, bufferProvider);
  }

  /**
   * @deprecated use constructor that takes globalDeadlineNanos.
   */
  @Deprecated
  public PipedOutputStream(final int bufferSize,
          final SizedRecyclingSupplier<byte[]> bufferProvider, @Nullable final Long globalDeadlineMillis) {
    this(globalDeadlineMillis == null ? null
            : Timing.getCurrentTiming().fromEpochMillisToNanoTime(globalDeadlineMillis), bufferSize, bufferProvider);
  }

  /**
   * Create a PipedOutputStream.
   * @param globalDeadlineNanos the deadline relative to System.nanoTime().
   * @param bufferSize the buffer size in bytes.
   * @param bufferProvider a buffer provider. (to allow more efficient recycling)
   */
  public PipedOutputStream(@Nullable final Long globalDeadlineNanos, final int bufferSize,
          final SizedRecyclingSupplier<byte[]> bufferProvider) {
    if (bufferSize < 2) {
      throw new IllegalArgumentException("Illegal buffer size " + bufferSize);
    }
    this.bufferProvider = bufferProvider;
    buffer = bufferProvider.get(bufferSize);
    startIdx = 0;
    endIdx = 0;
    readerPerceivedEndIdx = 0;
    writerClosed = false;
    nrReadStreams = 0;
    closedException = null;
    this.globalDeadlineNanos = globalDeadlineNanos;
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    long deadline = getNanoDeadline();
    writeUntil(b, off, len, deadline);
  }

  public long getNanoDeadline() {
    if (globalDeadlineNanos == null) {
      return ExecutionContexts.getContextDeadlineNanos();
    } else {
      return globalDeadlineNanos;
    }
  }

  public void writeUntil(final byte[] b, final int off, final int len, final long deadlineNanos) throws IOException {
    int bytesWritten = 0;
    while (bytesWritten < len) {
      synchronized (sync) {
        int a2w = 0;
        while (!writerClosed && (a2w = availableToWrite()) < 1) {
          long timeToWaitNanos = deadlineNanos - TimeSource.nanoTime();
          if (timeToWaitNanos <= 0) {
            throw new IOTimeoutException(deadlineNanos, -timeToWaitNanos);
          }
          try {
            TimeUnit.NANOSECONDS.timedWait(sync, timeToWaitNanos);
          } catch (InterruptedException ex) {
            throw new IOException("Interrupted while writing " + Arrays.toString(b), ex);
          }
        }
        if (writerClosed) {
          throw new IOException("Cannot write, stream closed " + this, closedException);
        }
        a2w = Math.min(a2w, len - bytesWritten);
        int wrToEnd = Math.min(a2w, buffer.length - endIdx);
        System.arraycopy(b, off + bytesWritten, buffer, endIdx, wrToEnd);
        endIdx += wrToEnd;
        bytesWritten += wrToEnd;
        int wrapArround = a2w - wrToEnd;
        if (wrapArround > 0) {
          System.arraycopy(b, off + bytesWritten, buffer, 0, wrapArround);
          endIdx = wrapArround;
          bytesWritten += wrapArround;
        } else if (endIdx >= buffer.length) {
          endIdx = 0;
        }
        if (availableToWrite() < 1) {
          flush();
        }
      }
    }
  }

  @Override
  public void write(final int b) throws IOException {
    long deadline = getNanoDeadline();
    writeUntil(b, deadline);
  }

  public void writeUntil(final int b, final long deadlineNanos) throws IOException {
    synchronized (sync) {
      int a2w = 0;
      while (!writerClosed && (a2w = availableToWrite()) < 1) {
        try {
          long timeToWaitNanos = deadlineNanos - TimeSource.nanoTime();
          if (timeToWaitNanos <= 0) {
            throw new IOTimeoutException(deadlineNanos, -timeToWaitNanos);
          }
          TimeUnit.NANOSECONDS.timedWait(sync, timeToWaitNanos);
        } catch (InterruptedException ex) {
          throw new IOException("Interrupted while writing " + b, ex);
        }
      }
      if (writerClosed) {
        throw new IOException("Cannot write stream closed " + this, closedException);
      }
      buffer[endIdx++] = (byte) b;
      if (endIdx >= buffer.length) {
        endIdx = 0;
      }
      if (a2w < 2) {
        flush();
      }
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
    if (startIdx <= readerPerceivedEndIdx) {
      return readerPerceivedEndIdx - startIdx;
    } else {
      return buffer.length - startIdx + readerPerceivedEndIdx;
    }
  }

  private int contentInBuffer() {
    if (startIdx <= endIdx) {
      return endIdx - startIdx;
    } else {
      return buffer.length - startIdx + endIdx;
    }
  }

  @Override
  public void flush() {
    synchronized (sync) {
      if (readerPerceivedEndIdx != endIdx) {
        readerPerceivedEndIdx = endIdx;
        sync.notifyAll();
      }
    }
  }

  @Override
  @DischargesObligation
  public void close() {
    synchronized (sync) {
      if (!writerClosed) {
        try {
          writerClosed = true;
          flush();
        } finally {
          if (nrReadStreams == 0 && availableToRead() == 0) {
            bufferProvider.recycle(buffer);
            buffer = null;
          }
          sync.notifyAll();
        }
      }
    }
  }


  /**
   * Close this piped output stream, and provide a exception reason.
   * Designed to propagate exception details from the consumer to the producer.
   * A consumer that reads data from a InputStream connected to this PipedOutpuStream,
   * can close this pipe, and provide a exception. THis exception will be used as cause for any
   * exceptions thrown by subsequent attempted write operations.
   * @param ex the exception to use as root cause.
   */
  @DischargesObligation
  public void close(final Exception ex) {
    synchronized (sync) {
      if (closedException != null) {
        ex.addSuppressed(closedException);
      }
      closedException = ex;
      close();
    }
  }


  public InputStream getInputStream() {
    synchronized (sync) {
      if (writerClosed && availableToRead() == 0) {
        return EmptyInputStream.EMPTY;
      }
      nrReadStreams++;
      return new PipedInputStream();
    }

  }

  public synchronized byte[] getUnreadBytesFromBuffer() {
    final int size = contentInBuffer();
    if (size == 0) {
      return org.spf4j.base.Arrays.EMPTY_BYTE_ARRAY;
    }
    byte[] result = new byte[size];
    if (startIdx < endIdx) {
      System.arraycopy(buffer, startIdx, result, 0, result.length);
    } else {
      final int toEnd = buffer.length - startIdx;
      System.arraycopy(buffer, startIdx, result, 0, toEnd);
      System.arraycopy(buffer, 0, result, toEnd, endIdx);
    }
    return result;
  }

  @Override
  public String toString() {
    synchronized (sync) {
      if (buffer == null) {
        return "PipedOutputStream{readers=" + nrReadStreams + ", startIdx=" + startIdx
                + ", endIdx=" + endIdx
                + ", readerPerceivedEndIdx=" + readerPerceivedEndIdx
                + ", closed=" + writerClosed + '}';
      } else {
        return "PipedOutputStream{readers=" + nrReadStreams + ", bufferLength="
                + buffer.length + ", startIdx=" + startIdx
                + ", endIdx=" + endIdx
                + ", readerPerceivedEndIdx=" + readerPerceivedEndIdx
                + ((writerClosed)
                        ? ", closed=" + writerClosed
                        : ", unread=" + BaseEncoding.base64().encode(getUnreadBytesFromBuffer()))
                + '}';
      }
    }
  }

  public final class PipedInputStream extends InputStream {

    private boolean readerClosed = false;

    private PipedInputStream() {
    }

    public PipedOutputStream getOutputStream() {
      return PipedOutputStream.this;
    }

    @Override
    public int read() throws IOException {
      long deadline = getNanoDeadline();
      return readUntil(deadline);
    }

    public int readUntil(final long deadlineNanos) throws IOException {
      synchronized (sync) {
        int availableToRead = 0;
        while (!readerClosed && (availableToRead = availableToRead()) < 1 && !writerClosed) {
          long timeToWaitNanos = deadlineNanos - TimeSource.nanoTime();
          if (timeToWaitNanos <= 0) {
            throw new IOTimeoutException(deadlineNanos, -timeToWaitNanos);
          }
          try {
            TimeUnit.NANOSECONDS.timedWait(sync, timeToWaitNanos);
          } catch (InterruptedException ex) {
            throw new IOException("Interrupted while reading from "
                    + PipedOutputStream.this, ex);
          }
        }
        if (readerClosed) {
          throw new IOException("Reader is closed for " + PipedOutputStream.this);
        }
        if (availableToRead == 0) {
          if (!writerClosed) {
            throw new IllegalStateException("Stream must be closed " + PipedOutputStream.this);
          }
          return -1;
        }
        int result = buffer[startIdx];
        startIdx++;
        if (startIdx >= buffer.length) {
          startIdx = 0;
        }
        sync.notifyAll();
        return result;
      }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      long deadline = getNanoDeadline();
      return readUntil(len, b, off, deadline);
    }

    public int readUntil(final int len, final byte[] b, final int off, final long deadline)
            throws IOException {
      int bytesWritten = 0;
      synchronized (sync) {
        int availableToRead = 0;
        while (!readerClosed && (availableToRead = availableToRead()) < 1 && !writerClosed) {
          long timeToWaitNanos = deadline - TimeSource.nanoTime();
          if (timeToWaitNanos <= 0) {
            throw new IOTimeoutException(deadline, -timeToWaitNanos);
          }
          try {
            TimeUnit.NANOSECONDS.timedWait(sync, timeToWaitNanos);
          } catch (InterruptedException ex) {
            throw new IOException("Interrupted while reading from " + PipedOutputStream.this, ex);
          }
        }
        if (readerClosed) {
          throw new IOException("Reader is closed for " + PipedOutputStream.this);
        }
        if (availableToRead == 0) {
          if (!writerClosed) {
            throw new IllegalStateException("Stream should be closed, " + PipedOutputStream.this);
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
        sync.notifyAll();
        return bytesWritten;
      }
    }

    @Override
    public int available() {
      synchronized (sync) {
        if (readerClosed) {
          throw new UncheckedIOException("Reader is closed for " + PipedOutputStream.this, null);
        }
        return availableToRead();
      }
    }

    @Override
    public void close() {
      synchronized (sync) {
        nrReadStreams--;
        readerClosed = true;
        if (writerClosed && nrReadStreams == 0 && availableToRead() == 0) {
          bufferProvider.recycle(buffer);
          buffer = null;
        }
        sync.notifyAll();
      }
    }
  }

}
