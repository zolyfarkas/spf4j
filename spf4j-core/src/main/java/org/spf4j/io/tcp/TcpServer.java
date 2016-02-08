package org.spf4j.io.tcp;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Throwables;
import org.spf4j.ds.UpdateablePriorityQueue;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("HES_EXECUTOR_NEVER_SHUTDOWN")
@Beta
public final class TcpServer implements Closeable {

    private final ExecutorService executor;

    private final ClientHandler handlerFactory;

    private final int serverPort;

    private final int acceptBacklog;

    private volatile boolean terminated;

    private volatile Selector selector;

    private volatile Future<?> server;

    private volatile CountDownLatch started;

    public TcpServer(final ExecutorService executor, final ClientHandler handlerFactory,
            final int serverPort,
            final int acceptBacklog) {
        this.executor = executor;
        this.handlerFactory = handlerFactory;
        this.acceptBacklog = acceptBacklog;
        this.serverPort = serverPort;
        this.terminated = false;
        this.selector = null;
        this.started = null;
        this.server = null;
    }

    @SuppressFBWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
    public void run() throws IOException {
        Selector sel = Selector.open();
        selector = sel;
        try (ServerSocketChannel serverCh = ServerSocketChannel.open()) {

            serverCh.bind(new InetSocketAddress(serverPort), acceptBacklog);
            serverCh.configureBlocking(false);
            BlockingQueue<Runnable> tasksToRunBySelector = new ArrayBlockingQueue<>(64);
            UpdateablePriorityQueue<DeadlineAction> deadlineActions =
                    new UpdateablePriorityQueue<>(64, DeadlineAction.COMPARATOR);
            new AcceptorSelectorEventHandler(serverCh, handlerFactory, sel, executor,
                    tasksToRunBySelector, deadlineActions)
                    .initialInterestRegistration();
            started.countDown();
            while (!terminated) {
                int nrSelectors = sel.select(100);
                if (nrSelectors > 0) {
                    Set<SelectionKey> selectedKeys = sel.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey skey = keyIterator.next();
                        final Object attachment = skey.attachment();
                        if (attachment instanceof SelectorEventHandler) {
                            SelectorEventHandler seh = (SelectorEventHandler) attachment;
                            if (seh.canRunAsync()) {
                                seh.runAsync(skey);
                            } else {
                                seh.run(skey);
                            }
                        }
                        keyIterator.remove();
                    }
                }
                // process deadlineActions
                long currentTime = System.currentTimeMillis();
                DeadlineAction peek;
                //CHECKSTYLE:OFF
                while ((peek = deadlineActions.peek()) != null && currentTime > peek.getDeadline()) {
                    deadlineActions.poll().getAction().run();
                }
                //CHECKSTYLE:ON
                Runnable task;
                while ((task = tasksToRunBySelector.poll()) != null) {
                    task.run();
                }
            }
        } finally {
            try {
                closeSelectorChannels(selector);
            } catch (IOException ex) {
                try {
                    sel.close();
                } catch (IOException ex2) {
                    ex2.addSuppressed(ex);
                    throw ex2;
                }
                throw ex;
            }
            sel.close();
        }
    }

    public synchronized void runInBackground() {

        if (server == null) {
            terminated = false;
            started = new CountDownLatch(1);
            server = executor.submit(new AbstractRunnable(false) {
                @Override
                public void doRun() throws IOException {
                    TcpServer.this.run();
                }
            });
        } else {
            throw new IllegalStateException("There is already a running server " + server);
        }
    }


    public void waitForStared() throws InterruptedException {
        started.await();
    }


    @Override
    public synchronized void close() throws IOException {
        if (!terminated) {
            try {
                terminated = true;
                selector.wakeup();
                Future<?> svr = server;
                try {
                    svr.get(1000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ex) {
                   svr.cancel(true);
                } finally {
                    server = null;
                }
            } catch (InterruptedException | ExecutionException ex) {
                throw new IOException(ex);
            }
        }
    }


    public static void closeSelectorChannels(final Selector selector) throws IOException {
        IOException ex = null;
        for (SelectionKey key : selector.keys()) {
            SelectableChannel channel = key.channel();
            try {
                channel.close();
            } catch (IOException ex2) {
                if (ex == null) {
                    ex = ex2;
                } else {
                    ex = Throwables.suppress(ex, ex2);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    @Override
    public String toString() {
        return "TcpServer{" + "executor=" + executor + ", handlerFactory=" + handlerFactory
                + ", serverPort=" + serverPort + ", acceptBacklog=" + acceptBacklog
                + ", terminated=" + terminated + ", selector=" + selector + ", server=" + server + '}';
    }




}
