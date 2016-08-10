/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
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

    private final UpdateablePriorityQueue.ElementRef deadlineActionRef;

    private volatile boolean connected;

    private final ExecutorService exec;

    private final Runnable readRun;
    private final Runnable writeRun;

    private final SnifferFactory snifferFactory;

    private final TransferBuffer in;

    private final TransferBuffer out;

    private final BlockingQueue<Runnable> tasksToRunBySelector;

    private static final Logger LOG = LoggerFactory.getLogger(ProxyBufferTransferHandler.class);

    public ProxyBufferTransferHandler(final TransferBuffer in, final TransferBuffer out,
            @Nullable final SnifferFactory snifferFactory,
            final SocketChannel channel, final Selector selector, final ExecutorService exec,
            final BlockingQueue<Runnable> tasksToRunBySelector,
            final UpdateablePriorityQueue.ElementRef deadlineActionRef) {
        this.in = in;
        this.out = out;
        this.exec = exec;
        this.channel = channel;
        this.selector = selector;
        this.deadlineActionRef = deadlineActionRef;
        this.connected = channel.isConnected();
        this.snifferFactory = snifferFactory;
        this.tasksToRunBySelector = tasksToRunBySelector;
        readRun = new ReadFromChannel(in, channel);
        writeRun = new WriteToChannel(out, channel);
    }

    @Override
    public SelectionKey initialInterestRegistration() throws ClosedChannelException {
        SelectionKey tkey = channel.register(selector, SelectionKey.OP_READ
                | SelectionKey.OP_CONNECT, this);
        final ReadInterest readInterest = new ReadInterest(tkey);
        final WriteInterest writeInterest = new WriteInterest(tkey);
        out.setIsDataInBufferHook(new DataAvailableToWriteHook(tasksToRunBySelector, writeInterest, selector));
        in.setIsRoomInBufferHook(new RoomToReadHook(tasksToRunBySelector, readInterest, selector));
        return tkey;
    }

    @Override
    public boolean canRunAsync() {
        return true;
    }

    @Override
    public synchronized void runAsync(final SelectionKey sKey) throws IOException {
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
    public void run(final SelectionKey skey) {
        throw new UnsupportedOperationException();
    }

    private static class ReadInterest implements Runnable {

        private final SelectionKey tKey;

        ReadInterest(final SelectionKey key) {
            tKey = key;
        }

        @Override
        public void run() {
            tKey.interestOps(tKey.interestOps() | SelectionKey.OP_READ);
        }
    }

    private static class WriteInterest implements Runnable {

        private final SelectionKey tKey;

        WriteInterest(final SelectionKey key) {
            this.tKey = key;
        }

        @Override
        public void run() {
            tKey.interestOps(tKey.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    private static class DataAvailableToWriteHook extends AbstractRunnable {

        private final BlockingQueue<Runnable> tasksToRunBySelector;
        private final WriteInterest writeInterest;
        private final Selector selector;

        DataAvailableToWriteHook(final BlockingQueue<Runnable> tasksToRunBySelector,
                final WriteInterest writeInterest, final Selector selector) {
            super(false);
            this.tasksToRunBySelector = tasksToRunBySelector;
            this.writeInterest = writeInterest;
            this.selector = selector;
        }

        @Override
        public void doRun() throws InterruptedException  {
            tasksToRunBySelector.put(writeInterest);
            selector.wakeup();
        }
    }

    private static class RoomToReadHook extends AbstractRunnable {

        private final BlockingQueue<Runnable> tasksToRunBySelector;
        private final ReadInterest readInterest;
        private final Selector selector;

        RoomToReadHook(final BlockingQueue<Runnable> tasksToRunBySelector,
                final ReadInterest readInterest, final Selector selector) {
            super(false);
            this.tasksToRunBySelector = tasksToRunBySelector;
            this.readInterest = readInterest;
            this.selector = selector;
        }

        @Override
        public void doRun() throws InterruptedException {
            tasksToRunBySelector.put(readInterest);
            selector.wakeup();
        }
    }

    private static class ReadFromChannel extends AbstractRunnable {

        private final TransferBuffer in;
        private final SocketChannel channel;

        ReadFromChannel(final TransferBuffer in, final SocketChannel channel) {
            super(true);
            this.in = in;
            this.channel = channel;
        }

      @Override
      public void doRun() {
        int read = in.read(channel);
        LOG.debug("Read {} bytes from {}", read, channel);
      }
    }

    private static class WriteToChannel extends AbstractRunnable {

        private final TransferBuffer out;
        private final SocketChannel channel;

        WriteToChannel(final TransferBuffer out, final SocketChannel channel) {
            super(true);
            this.out = out;
            this.channel = channel;
        }

        @Override
        public void doRun() {
                int written = out.write(channel);
                LOG.debug("Written {} bytes to {}", written, channel);
        }
    }

    @Override
    public String toString() {
        return "ProxyBufferTransferHandler{" + "channel=" + channel + ", selector=" + selector
                + ", connected=" + connected + ", in=" + in + ", out=" + out + '}';
    }

}
