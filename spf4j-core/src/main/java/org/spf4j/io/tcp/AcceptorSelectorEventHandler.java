
package org.spf4j.io.tcp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.ds.UpdateablePriorityQueue;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("HES_EXECUTOR_NEVER_SHUTDOWN")
public final class AcceptorSelectorEventHandler extends SelectorEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptorSelectorEventHandler.class);

    private final ClientHandler clientHandler;
    private final ServerSocketChannel serverChannel;
    private final Selector selector;
    private final ExecutorService exec;
    private final BlockingQueue<Runnable> tasksToRunBySelector;
    private final UpdateablePriorityQueue<DeadlineAction> deadlineActions;

    public AcceptorSelectorEventHandler(final ServerSocketChannel channel,
            final ClientHandler clientHandler, final Selector selector, final ExecutorService exec,
            final BlockingQueue<Runnable> tasksToRunBySelector,
            final UpdateablePriorityQueue<DeadlineAction> deadlineActions) {
        this.serverChannel = channel;
        this.clientHandler = clientHandler;
        this.selector = selector;
        this.exec = exec;
        this.tasksToRunBySelector = tasksToRunBySelector;
        this.deadlineActions = deadlineActions;
    }

    @Override
    public void run(final SelectionKey key) throws IOException {
            if (!key.isAcceptable()) {
                throw new IllegalStateException("selection key must be acceptable " + key);
            }
            SocketChannel clientChannel;
            while ((clientChannel = serverChannel.accept()) != null) {
                try {
                    LOG.debug("Accepted {}", clientChannel);
                    clientChannel.configureBlocking(false);
                    clientHandler.handle(selector, clientChannel, exec, tasksToRunBySelector, deadlineActions);
                } catch (IOException ex) {
                    clientChannel.close();
                    throw ex;
                }
            }
    }


    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public void runAsync(final SelectionKey key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SelectionKey initialInterestRegistration() throws ClosedChannelException {
        return serverChannel.register(selector, SelectionKey.OP_ACCEPT, this);
    }

    @Override
    public String toString() {
        return "AcceptorSelectorEventHandler{" + "clientHandler=" + clientHandler + ", serverChannel="
                + serverChannel + ", selector=" + selector + ", exec=" + exec + ", tasksToRunBySelector="
                + tasksToRunBySelector + ", deadlineActions=" + deadlineActions + '}';
    }



}
