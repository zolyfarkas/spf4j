package org.spf4j.io.tcp.proxy;

import com.google.common.net.HostAndPort;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.ds.UpdateablePriorityQueue;
import org.spf4j.io.tcp.ClientHandler;
import org.spf4j.io.tcp.DeadlineAction;

/**
 *
 * @author zoly
 */
public final class ProxyClientHandler implements ClientHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyClientHandler.class);

    private final HostAndPort fwdDestination;
    private final int proxyBufferSize;
    private final int connectTimeoutMillis;


    public ProxyClientHandler(final HostAndPort fwdDestination, final int proxyBufferSize,
            final int connectTimeoutMillis) {
        this.fwdDestination = fwdDestination;
        this.proxyBufferSize = proxyBufferSize;
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    @Override
    public void handle(final Selector serverSelector, final SocketChannel clientChannel,
            final ExecutorService exec, final BlockingQueue<Runnable> tasksToRunBySelector,
            final UpdateablePriorityQueue<DeadlineAction> deadlineActions)  throws IOException {
        final InetSocketAddress socketAddress = new InetSocketAddress(
                fwdDestination.getHostText(), fwdDestination.getPort());
        final SocketChannel proxyChannel = SocketChannel.open();
        proxyChannel.configureBlocking(false);
        proxyChannel.connect(socketAddress);
        TransferBuffer c2s = new TransferBuffer(proxyBufferSize);
        TransferBuffer s2c = new TransferBuffer(proxyBufferSize);
        final long connectDeadline = System.currentTimeMillis() + connectTimeoutMillis;
        UpdateablePriorityQueue.ElementRef daction = deadlineActions.add(new DeadlineAction(connectDeadline,
                new CloseChannelsOnTimeout(proxyChannel, clientChannel)));
        new ProxyBufferTransferHandler(c2s, s2c, clientChannel,
                serverSelector, exec, tasksToRunBySelector, daction).initialInterestRegistration();
        new ProxyBufferTransferHandler(s2c, c2s, proxyChannel,
                serverSelector, exec, tasksToRunBySelector, daction).initialInterestRegistration();

    }

    static final class CloseChannelsOnTimeout extends AbstractRunnable {

        private final SocketChannel proxyChannel;
        private final SocketChannel clientChannel;

        public CloseChannelsOnTimeout(final SocketChannel proxyChannel, final SocketChannel clientChannel) {
            super(true);
            this.proxyChannel = proxyChannel;
            this.clientChannel = clientChannel;
        }

        @Override
        public void doRun() throws IOException {
            LOG.warn("Timed out connecting to {}", proxyChannel);
            try {
                clientChannel.close();
            } finally {
                proxyChannel.close();
            }
        }
    }

}
