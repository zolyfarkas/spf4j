package org.spf4j.zel.vm;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TODO: there is some dumb sync in this class that needs to be reviewed.
 *
 * @author zoly
 */
public final class Channel {

    public static final Object EOF = new Object(); 
    
    private final BlockingQueue<Object> queue;

    private final BlockingQueue<VMFuture<Object>> readers;

    private final VMExecutor exec;
    
    private boolean closed;

    public Channel(final VMExecutor exec) {
        this.queue = new LinkedBlockingDeque<Object>();
        this.readers = new LinkedBlockingQueue<VMFuture<Object>>();
        this.exec = exec;
        this.closed = false;
    }

    public Object read() throws InterruptedException {
        synchronized (this) {
            if (closed) {
                return EOF;
            }
            Object obj = queue.poll();
            if (obj == null) {
                VMASyncFuture<Object> fut = new VMASyncFuture<Object>();
                readers.put(fut);
                return fut;
            } else {
                return obj;
            }
        }
    }

    public void write(final Object obj) throws InterruptedException {
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException("Channel is closed, cannot write into it");
            }
            VMFuture<Object> reader = readers.poll();
            if (reader != null) {
                reader.setResult(obj);
                exec.resumeSuspendables(reader);
            } else {
                queue.put(obj);
            }
        }
    }
    
    public void close() {
        synchronized(this) {
            VMFuture<Object> reader = readers.poll();
            while (reader != null) { 
                reader.setResult(EOF);
                exec.resumeSuspendables(reader);
            }
            closed = true;
        }
    }

    public static class Factory implements Method {

        @Override
        public Object invoke(final ExecutionContext context, final Object[] parameters) throws Exception {
            return new Channel(context.execService);
        }

    }
}
