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
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("VO_VOLATILE_REFERENCE_TO_ARRAY")
public final class SimpleExecutor implements Executor {

    private final BlockingQueue<Runnable> queuedTasks;

    private final ThreadFactory threadFactory;

    private volatile boolean terminated;

    private volatile Thread [] threads;

    private static final Logger LOG = LoggerFactory.getLogger(SimpleExecutor.class);

    public SimpleExecutor() {
        queuedTasks = new LinkedBlockingQueue<>();
        terminated = false;
        threads = null;
        threadFactory = new CustomThreadFactory("SimpleExecutor", true);
    }

    public void startThreads(final int nrThreads) {
        Thread [] newThreads = new Thread[nrThreads];
        for (int i = 0; i < nrThreads; i++) {
            Thread t = threadFactory.newThread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (!terminated) {
                            Runnable runnable = queuedTasks.poll(1, TimeUnit.SECONDS);
                            if (runnable != null) {
                                try {
                                    runnable.run();
                                } catch (RuntimeException ex) {
                                    LOG.error("Exception encountered in worker", ex);
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        LOG.info("SimpleExecutor worker interrupted", ex);
                    }
                }
            });
            t.start();
            newThreads[i] = t;
        }
        threads = newThreads;
    }

    public void shutdown() {
        terminated = true;
    }

    public void shutdownAndWait(final long timeout) throws InterruptedException {
        terminated = true;
        long deadline = timeout + System.currentTimeMillis();
        Thread [] theThreads = threads;
        for (Thread t : theThreads) {
            t.join(deadline - System.currentTimeMillis());
        }
    }


    @Override
    public void execute(final Runnable command) {
        try {
            queuedTasks.put(command);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

}
