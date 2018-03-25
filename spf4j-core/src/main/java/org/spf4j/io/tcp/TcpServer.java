/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.io.tcp;

import com.google.common.annotations.Beta;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Closeables;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.RestartableServiceImpl;
import org.spf4j.ds.UpdateablePriorityQueue;
import org.spf4j.failsafe.RetryPolicy;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("HES_EXECUTOR_NEVER_SHUTDOWN")
@Beta
public final class TcpServer extends RestartableServiceImpl {

  private static final Logger LOG = LoggerFactory.getLogger(TcpServer.class);

  private final int serverPort;

    public TcpServer(final ExecutorService executor, final ClientHandler handlerFactory,
          final int serverPort,
          final int acceptBacklog) {
      this(executor, handlerFactory, serverPort, acceptBacklog, 60000);
    }

  public TcpServer(final ExecutorService executor, final ClientHandler handlerFactory,
          final int serverPort,
          final int acceptBacklog, final int bindTimeoutMillis) {
    super(new Supplier<Service>() {
      @Override
      public Service get() {
        return new TcpServerGuavaService(executor, handlerFactory, serverPort, acceptBacklog, bindTimeoutMillis);
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

    private final int bindTimeoutMillis;

    private volatile boolean shouldRun;

    private volatile Selector selector;

    private volatile ServerSocketChannel serverCh;

    public TcpServerGuavaService(final ExecutorService executor, final ClientHandler handlerFactory,
            final int serverPort,
            final int acceptBacklog,
            final int bindTimeoutMillis) {
      this.executor = executor;
      this.handlerFactory = handlerFactory;
      this.acceptBacklog = acceptBacklog;
      this.serverPort = serverPort;
      this.shouldRun = true;
      this.selector = null;
      this.bindTimeoutMillis = bindTimeoutMillis;
    }

    @Override
    protected void startUp() throws Exception {
      selector = Selector.open();
      try {
        serverCh = RetryPolicy.defaultPolicy().call(() -> {
          ServerSocketChannel sc = ServerSocketChannel.open();
            try {
              sc.bind(new InetSocketAddress(serverPort), acceptBacklog);
              sc.configureBlocking(false);
              return sc;
            } catch (IOException | RuntimeException e) {
              sc.close();
              throw e;
            }
        }, IOException.class, bindTimeoutMillis, TimeUnit.MILLISECONDS);
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
        while (shouldRun) {
          int nrSelectors = sel.select(100);
          if (nrSelectors > 0) {
            Set<SelectionKey> selectedKeys = sel.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while (keyIterator.hasNext()) {
              SelectionKey skey = keyIterator.next();
              final Object attachment = skey.attachment();
              if (attachment instanceof SelectorEventHandler) {
                SelectorEventHandler seh = (SelectorEventHandler) attachment;
                try {
                  if (seh.canRunAsync()) {
                    seh.runAsync(skey);
                  } else {
                    seh.run(skey);
                  }
                } catch (CancelledKeyException ex) {
                  LOG.debug("Canceled key {}", skey, ex);
                }
              }
              keyIterator.remove();
            }
          }
          // process deadlineActions
          long currentTime = TimeSource.nanoTime();
          DeadlineAction peek;
          //CHECKSTYLE:OFF
          while ((peek = deadlineActions.peek()) != null && (peek.getDeadline() - currentTime <= 0)) {
            deadlineActions.poll().getAction().run();
          }
          //CHECKSTYLE:ON
          Runnable task;
          while ((task = tasksToRunBySelector.poll()) != null) {
            task.run();
          }
        }
      } finally {
        IOException closeAll =
                Closeables.closeAll(Closeables.closeSelectorChannels(sel), sel, serverCh);
        if (closeAll != null) {
          throw closeAll;
        }
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
      shouldRun = false;
      selector.wakeup();
    }

    @Override
    public void close() throws IOException {
      this.stopAsync().awaitTerminated();
    }


    @Override
    public String toString() {
      return "TcpServer{" + "executor=" + executor + ", handlerFactory=" + handlerFactory
              + ", serverPort=" + serverPort + ", acceptBacklog=" + acceptBacklog
              + ", shouldRun=" + shouldRun + ", selector=" + selector + '}';
    }
  }
}
