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
    
    private Runnable newDataInBufferHook;

//    private Runnable bufferFilledHook;
//
//    private Runnable bufferHasRoomHook;

    public TransferBuffer(final int bufferSize) {
        buffer = ByteBuffer.allocateDirect(bufferSize);
        lastOperation = Operation.READ;
        isEof = false;
        this.newDataInBufferHook = null;
    }

    public synchronized int read(final SocketChannel channel) throws IOException {
        if (lastOperation == Operation.WRITE) {
            buffer.compact();
            lastOperation = Operation.READ;
        }
        int nrRead = channel.read(buffer);
        if (nrRead < 0) {
            isEof = true;
            channel.socket().shutdownInput(); // ? is this really necessary?
        } else if (nrRead > 0) {
            if (newDataInBufferHook != null) {
                newDataInBufferHook.run();
            }
        }
//        if (!buffer.hasRemaining() && newDataInBufferHook != null) {
//            bufferFilledHook.run();
//        }
        return nrRead;
    }

    public synchronized int write(final SocketChannel channel) throws IOException {
        if (lastOperation == Operation.READ) {
            buffer.flip();
            lastOperation = Operation.WRITE;
        }
        int nrWritten = channel.write(buffer);
        if (!buffer.hasRemaining()) {
            if (isEof) {
                channel.socket().shutdownOutput();
            }
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

    public synchronized Runnable getNewDataInBufferHook() {
        return newDataInBufferHook;
    }

    public synchronized void setNewDataInBufferHook(final Runnable newDataInBufferHook) {
        this.newDataInBufferHook = newDataInBufferHook;
    }

    @Override
    public String toString() {
        return "TransferBuffer{" + "buffer=" + buffer + ", lastOperation=" + lastOperation
                + ", isEof=" + isEof + ", newDataInBufferHook=" + newDataInBufferHook + '}';
    }

}
