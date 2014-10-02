package org.spf4j.zel.vm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedList;
import java.util.Queue;

/**
 * TODO: there is some dumb sync in this class that needs to be reviewed.
 *
 * @author zoly
 */
@SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
public final class Channel {

    public static final Object EOF = new Object();
    
    private final Queue<Object> queue;

    private final Queue<VMFuture<Object>> readers;

    private final VMExecutor exec;
    
    private boolean closed;

    public Channel(final VMExecutor exec) {
        this.queue = new LinkedList<Object>();
        this.readers = new LinkedList<VMFuture<Object>>();
        this.exec = exec;
        this.closed = false;
    }

    @SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
    public Object read() throws InterruptedException {
        synchronized (this) {
            Object obj = queue.poll();
            if (obj == null) {
                VMASyncFuture<Object> fut = new VMASyncFuture<Object>();
                readers.add(fut);
                return fut;
            } else {
                if (obj == EOF) {
                    queue.add(EOF);
                }
                return obj;
            }
        }
    }

    public void write(final Object obj) throws InterruptedException {
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException("Channel is closed, cannot write " + obj + " into it");
            }
            VMFuture<Object> reader = readers.poll();
            if (reader != null) {
                reader.setResult(obj);
                exec.resumeSuspendables(reader);
            } else {
                queue.add(obj);
            }
        }
    }
    
    public void close() {
        synchronized (this) {
            VMFuture<Object> reader;
            while ((reader = readers.poll()) != null) {
                reader.setResult(EOF);
                exec.resumeSuspendables(reader);
            }
            queue.add(EOF);
            closed = true;
        }
    }

    public static final class Factory implements Method {

        private Factory() { }
        
        @Override
        public Object invoke(final ExecutionContext context, final Object[] parameters) {
            return new Channel(context.execService);
        }
        
        public static final Factory INSTANCE = new Factory();

    }
}
