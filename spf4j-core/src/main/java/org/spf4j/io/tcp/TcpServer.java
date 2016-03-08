package org.spf4j.io.tcp;

import com.google.common.annotations.Beta;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.spf4j.base.Throwables;
import org.spf4j.concurrent.RestartableServiceImpl;
import org.spf4j.ds.UpdateablePriorityQueue;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("HES_EXECUTOR_NEVER_SHUTDOWN")
@Beta
public final class TcpServer extends RestartableServiceImpl {

    private final int serverPort;

    public TcpServer(final ExecutorService executor, final ClientHandler handlerFactory,
                final int serverPort,
                final int acceptBacklog)  {
        super(new Supplier<Service>() {
            @Override
            public Service get() {
                return new TcpServerGuavaService(executor, handlerFactory, serverPort, acceptBacklog);
            }
        });
        this.serverPort = serverPort;
    }

    @Override
    public String getServiceName() {
        return "TCP:LISTEN:" + serverPort;
    }

    public static final class TcpServerGuavaService extends AbstractExecutionThreadService
            implements Closeable {

        private final ExecutorService executor;

        private final ClientHandler handlerFactory;

        private final int serverPort;

        private final int acceptBacklog;

        private volatile boolean terminated;

        private volatile Selector selector;

        private volatile ServerSocketChannel serverCh;

        public TcpServerGuavaService(final ExecutorService executor, final ClientHandler handlerFactory,
                final int serverPort,
                final int acceptBacklog) {
            this.executor = executor;
            this.handlerFactory = handlerFactory;
            this.acceptBacklog = acceptBacklog;
            this.serverPort = serverPort;
            this.terminated = false;
            this.selector = null;
        }

        @Override
        protected void startUp() throws Exception {
            selector = Selector.open();
            try {
                ServerSocketChannel sc = ServerSocketChannel.open();
                try {
                    sc.bind(new InetSocketAddress(serverPort), acceptBacklog);
                    sc.configureBlocking(false);
                    serverCh = sc;
                } catch (IOException | RuntimeException e) {
                    sc.close();
                    throw e;
                }
            } catch (IOException | RuntimeException e) {
                selector.close();
                throw e;
            }
        }

        @SuppressFBWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
        @Override
        public void run() throws IOException {
            Selector sel = selector;
            try {
                BlockingQueue<Runnable> tasksToRunBySelector = new ArrayBlockingQueue<>(64);
                UpdateablePriorityQueue<DeadlineAction> deadlineActions
                        = new UpdateablePriorityQueue<>(64, DeadlineAction.COMPARATOR);
                new AcceptorSelectorEventHandler(serverCh, handlerFactory, sel, executor,
                        tasksToRunBySelector, deadlineActions)
                        .initialInterestRegistration();
                while (isRunning()) {
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

        @Override
        protected Executor executor() {
            return this.executor;
        }

        @Override
        protected String serviceName() {
            return "TCP:LISTEN:" + serverPort;
        }

        @Override
        protected void triggerShutdown() {
            selector.wakeup();
        }

        @Override
        public synchronized void close() throws IOException {
            this.stopAsync().awaitTerminated();
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
                    + ", terminated=" + terminated + ", selector=" + selector + '}';
        }
    }
}
