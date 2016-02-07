package org.spf4j.io.tcp.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author zoly
 */
public final class TransferBuffer {

    public enum Operation { READ, WRITE };


    private final ByteBuffer buffer;


    private Operation lastOperation;

    private boolean isEof;
    
    private Runnable isDataInBufferHook;

    private Runnable isRoomInBufferHook;

    private Sniffer incomingSniffer;


    public TransferBuffer(final int bufferSize) {
        buffer = ByteBuffer.allocateDirect(bufferSize);
        lastOperation = Operation.READ;
        isEof = false;
        this.isDataInBufferHook = null;
        this.isRoomInBufferHook = null;
    }

    public synchronized int read(final SocketChannel channel) throws IOException {
        if (lastOperation == Operation.WRITE) {
            buffer.compact();
            lastOperation = Operation.READ;
        }
        int nrRead = channel.read(buffer);
        if (nrRead > 1) {
            if (incomingSniffer != null) {
                incomingSniffer.received(buffer);
            }
        }
        if (nrRead < 0) {
            isEof = true;
            channel.socket().shutdownInput(); // ? is this really necessary?
        } else if (buffer.hasRemaining()) {
            isRoomInBufferHook.run();
        }
        if (buffer.position() > 0 || isEof) {
            isDataInBufferHook.run();
        }
        return nrRead;
    }

    public synchronized int write(final SocketChannel channel) throws IOException {
        if (lastOperation == Operation.READ) {
            buffer.flip();
            lastOperation = Operation.WRITE;
        }
        int nrWritten = channel.write(buffer);
        final boolean hasRemaining = buffer.hasRemaining();
        if (!hasRemaining && isEof) {
            channel.socket().shutdownOutput();
            return nrWritten;
        }
        if (buffer.position() > 0 && !isEof) {
           isRoomInBufferHook.run();
        }
        if (hasRemaining) {
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
