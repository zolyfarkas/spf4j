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
package org.spf4j.io.tcp.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public final class TransferBuffer {

  public enum Operation {
    READ, WRITE
  };

  private final ByteBuffer buffer;

  private Operation lastOperation;

  private boolean isEof;

  private Runnable isDataInBufferHook;

  private Runnable isRoomInBufferHook;

  private Sniffer incomingSniffer;

  private IOException readException;

  private IOException writeException;

  public TransferBuffer(final int bufferSize) {
    buffer = ByteBuffer.allocateDirect(bufferSize);
    lastOperation = Operation.READ;
    isEof = false;
    this.isDataInBufferHook = null;
    this.isRoomInBufferHook = null;
    this.readException = null;
    this.writeException = null;
  }

  private static final Logger LOG = LoggerFactory.getLogger(TransferBuffer.class);

  public synchronized int read(final SocketChannel channel) {
    if (lastOperation == Operation.WRITE) {
      buffer.compact();
      lastOperation = Operation.READ;
    }
    int nrRead;
    IOException oex = null;
    try {
      nrRead = channel.read(buffer);
      if (incomingSniffer != null && (nrRead != 0)) {
        nrRead = incomingSniffer.received(buffer, nrRead);
      }
    } catch (IOException ex) {
      oex = ex;
      readException = incomingSniffer.received(ex);
      if (readException != null) {
        LOG.debug("Exception while reading from {}", channel, ex);
        nrRead = -1;
      } else {
        nrRead = 0; // sniffer filters exception, behave a s no data received.
      }
      try {
        channel.close();
      } catch (IOException ex1) {
        if (readException != null) {
          readException.addSuppressed(ex1);
        }
      }
    }
    if (nrRead < 0) {
      isEof = true;
      try {
        channel.socket().shutdownInput(); // ? is this really necessary?
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    } else if (oex == null && buffer.hasRemaining()) {
      isRoomInBufferHook.run();
    }
    if (buffer.position() > 0 || isEof) {
      isDataInBufferHook.run();
    }
    return nrRead;
  }

  public synchronized int write(final SocketChannel channel) {
    if (lastOperation == Operation.READ) {
      buffer.flip();
      lastOperation = Operation.WRITE;
    }
    int nrWritten;
    try {
      nrWritten = channel.write(buffer);
    } catch (IOException ex) {
      try {
        channel.close();
      } catch (IOException ex1) {
        ex.addSuppressed(ex1);
      }
      LOG.debug("Exception while writing to {}", channel, ex);
      writeException = ex;
      nrWritten = 0;
    }
    final boolean hasRemaining = buffer.hasRemaining();
    if (!hasRemaining) {
      if (isEof) {
        try {
          channel.socket().shutdownOutput();
        } catch (ClosedChannelException closed) {
          //channel is closed already
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        return nrWritten;
      } else if (readException != null) {
        try {
          channel.socket().shutdownOutput();
        } catch (ClosedChannelException closed) {
          //channel is closed already
        }  catch (IOException ex) {
          readException.addSuppressed(ex);
        }
        LOG.debug("Closed channel {} due to read exception", channel, readException);
        return nrWritten;
      }
    }
    if (!isEof && buffer.position() > 0) {
      isRoomInBufferHook.run();
    }
    if (hasRemaining && writeException != null) {
      isDataInBufferHook.run();
    }
    return nrWritten;
  }

  public static int transfer(final SocketChannel in, final SocketChannel out, final ByteBuffer buffer)
          throws IOException {
    int read = 0;
    //CHECKSTYLE:OFF
    while ((in.finishConnect() && ((read = in.read(buffer)) > 0)) || (buffer.position() > 0)) {
      //CHECKSTYLE:ON
      buffer.flip();
      if (!out.finishConnect()) {
        break;
      }
      int writen = out.write(buffer);
      buffer.compact();
      if (writen <= 0) {
        break;
      }
    }
    return read;
  }

  public synchronized void setIsDataInBufferHook(final Runnable isDataInBufferHook) {
    this.isDataInBufferHook = isDataInBufferHook;
  }

  public synchronized void setIsRoomInBufferHook(final Runnable isRoomInBufferHook) {
    this.isRoomInBufferHook = isRoomInBufferHook;
  }

  public synchronized void setIncomingSniffer(final Sniffer incomingSniffer) {
    this.incomingSniffer = incomingSniffer;
  }

  @Override
  public String toString() {
    return "TransferBuffer{" + "buffer=" + buffer + ", lastOperation=" + lastOperation
            + ", isEof=" + isEof + ", isDataInBufferHook=" + isDataInBufferHook
            + ", isRoomInBufferHook=" + isRoomInBufferHook + '}';
  }

}
