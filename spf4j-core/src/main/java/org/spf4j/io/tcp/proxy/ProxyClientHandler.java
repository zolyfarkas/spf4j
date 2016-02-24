package org.spf4j.io.tcp.proxy;

import com.google.common.net.HostAndPort;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Closeables;
import org.spf4j.ds.UpdateablePriorityQueue;
import org.spf4j.io.tcp.ClientHandler;
import org.spf4j.io.tcp.DeadlineAction;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class ProxyClientHandler implements ClientHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyClientHandler.class);

    private final HostAndPort fwdDestination;
    private final int proxyBufferSize;
    private final int connectTimeoutMillis;
    private final SnifferFactory c2sSnifferFact;
    private final SnifferFactory s2cSnifferFact;

    /**
     * TCP proxy client handler.
     * @param fwdDestination - the destination all connections will be forwarded to.
     * @param c2sSnifferFact - create sniffer to be invoked when data is received from client.
     * @param s2cSnifferFact - create sniffer to be invoked when data is received from server.
     * @param proxyBufferSize - the transmission buffer sizes.
     * @param connectTimeoutMillis - The connection timeout.
     */

    public ProxyClientHandler(final HostAndPort fwdDestination,
        @Nullable final SnifferFactory c2sSnifferFact, @Nullable final SnifferFactory s2cSnifferFact,
        final int proxyBufferSize, final int connectTimeoutMillis) {
        this.fwdDestination = fwdDestination;
        this.proxyBufferSize = proxyBufferSize;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.c2sSnifferFact = c2sSnifferFact;
        this.s2cSnifferFact = s2cSnifferFact;
    }

    @Override
    public void handle(final Selector serverSelector, final SocketChannel clientChannel,
            final ExecutorService exec, final BlockingQueue<Runnable> tasksToRunBySelector,
            final UpdateablePriorityQueue<DeadlineAction> deadlineActions)  throws IOException {
        final InetSocketAddress socketAddress = new InetSocketAddress(
                fwdDestination.getHostText(), fwdDestination.getPort());
        final SocketChannel proxyChannel = SocketChannel.open();
        try {
            proxyChannel.configureBlocking(false);
            proxyChannel.connect(socketAddress);
            TransferBuffer c2s = new TransferBuffer(proxyBufferSize);
            if (c2sSnifferFact != null) {
                c2s.setIncomingSniffer(c2sSnifferFact.get(clientChannel));
            }
            TransferBuffer s2c = new TransferBuffer(proxyBufferSize);
            final long connectDeadline = System.currentTimeMillis() + connectTimeoutMillis;
            UpdateablePriorityQueue.ElementRef daction = deadlineActions.add(new DeadlineAction(connectDeadline,
                    new CloseChannelsOnTimeout(proxyChannel, clientChannel)));
            new ProxyBufferTransferHandler(c2s, s2c, null, clientChannel,
                    serverSelector, exec, tasksToRunBySelector, daction).initialInterestRegistration();
            new ProxyBufferTransferHandler(s2c, c2s, s2cSnifferFact, proxyChannel,
                    serverSelector, exec, tasksToRunBySelector, daction).initialInterestRegistration();
        } catch (IOException ex) {
            Closeables.closeAll(ex, proxyChannel, clientChannel);
        }

    }

    static final class CloseChannelsOnTimeout extends AbstractRunnable {

        private final SocketChannel proxyChannel;
        private final SocketChannel clientChannel;

        CloseChannelsOnTimeout(final SocketChannel proxyChannel, final SocketChannel clientChannel) {
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
