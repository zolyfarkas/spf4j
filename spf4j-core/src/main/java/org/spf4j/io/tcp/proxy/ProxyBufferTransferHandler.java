
package org.spf4j.io.tcp.proxy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.ds.UpdateablePriorityQueue;
import org.spf4j.io.tcp.SelectorEventHandler;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("HES_EXECUTOR_NEVER_SHUTDOWN")
@ParametersAreNonnullByDefault
public final class ProxyBufferTransferHandler extends SelectorEventHandler {

    private final SocketChannel channel;

    private final Selector selector;

    private volatile SelectionKey key;

    private final UpdateablePriorityQueue.ElementRef deadlineActionRef;

    private volatile boolean connected;

    private final ExecutorService exec;

    private final Runnable readRun;
    private final Runnable writeRun;

    private final SnifferFactory snifferFactory;

    private final TransferBuffer in;

    private static final Logger LOG = LoggerFactory.getLogger(ProxyBufferTransferHandler.class);


    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    public ProxyBufferTransferHandler(final TransferBuffer in, final TransferBuffer out,
            @Nullable final SnifferFactory snifferFactory,
            final SocketChannel channel, final Selector selector, final ExecutorService exec,
            final BlockingQueue<Runnable> tasksToRunBySelector,
            final UpdateablePriorityQueue.ElementRef deadlineActionRef) {
        this.in = in;
        this.key = null;
        this.exec = exec;
        this.channel = channel;
        this.selector = selector;
        this.deadlineActionRef = deadlineActionRef;
        this.connected = channel.isConnected();
        this.snifferFactory = snifferFactory;
        final Runnable readInterest = new Runnable() {
            @Override
            public void run() {
               final SelectionKey tKey = key;
               tKey.interestOps(tKey.interestOps() | SelectionKey.OP_READ);
            }
        };
        final Runnable writeInterest = new Runnable() {
            @Override
            public void run() {
               final SelectionKey tKey = key;
               tKey.interestOps(tKey.interestOps() | SelectionKey.OP_WRITE);
            }
        };
        out.setIsDataInBufferHook(new AbstractRunnable(false) {
            @Override
            public void doRun() throws Exception {
                tasksToRunBySelector.put(writeInterest);
                selector.wakeup();
            }
        });
        
        in.setIsRoomInBufferHook(new AbstractRunnable(false) {
            @Override
            public void doRun() throws Exception {
                tasksToRunBySelector.put(readInterest);
                selector.wakeup();
            }
        });

        readRun = new AbstractRunnable(true) {
            @Override
            public void doRun() throws IOException, InterruptedException {
                try {
                    int read = in.read(channel);
                    LOG.debug("Read {} bytes from {}", read, channel);
                } catch (IOException ex) {
                    try {
                        channel.close();
                    } catch (IOException ex1) {
                        ex1.addSuppressed(ex);
                        throw ex1;
                    }
                    throw ex;
                }
            }
        };

        writeRun = new AbstractRunnable(true) {
            @Override
            public void doRun() throws IOException, InterruptedException {
                try {
                    int written = out.write(channel);
                    LOG.debug("Written {} bytes to {}", written, channel);
                } catch (IOException ex) {
                    try {
                        channel.close();
                    } catch (IOException ex1) {
                        ex1.addSuppressed(ex);
                        throw ex1;
                    }
                    throw ex;
                }
            }
        };

    }

    @Override
    public SelectionKey initialInterestRegistration() throws ClosedChannelException {
        SelectionKey tkey = channel.register(selector, SelectionKey.OP_READ
                | SelectionKey.OP_CONNECT, this);
        key = tkey;
        return tkey;
    }

    @Override
    public boolean canRunAsync() {
        return true;
    }

    @Override
    public synchronized void runAsync() throws IOException {
        final SelectionKey sKey = key; // read volatile once
        if (!connected && sKey.isConnectable()) {
            sKey.interestOps(sKey.interestOps() & (~SelectionKey.OP_CONNECT));
            connected = channel.finishConnect();
            if (connected) {
                LOG.debug("Connected to {}", channel);
                deadlineActionRef.remove();
                if (snifferFactory != null) {
                    in.setIncomingSniffer(snifferFactory.get(channel));
                }
            }
        }
        if (connected) {
            if (sKey.isReadable()) {
                exec.execute(readRun);
                sKey.interestOps(sKey.interestOps() & (~SelectionKey.OP_READ));
            }
            if (sKey.isWritable()) {
                exec.execute(writeRun);
                sKey.interestOps(sKey.interestOps() & (~SelectionKey.OP_WRITE));
            }
        }
    }

    @Override
    public void run() throws IOException {
        throw new UnsupportedOperationException();
    }

}
