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
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.AbstractRunnable;

/**
 * This executor aims to be a general purpose executor for async tasks. (equivalent to ForkJoinPool.commonPool())
 *
 * @author zoly
 */
@SuppressFBWarnings("HES_EXECUTOR_NEVER_SHUTDOWN") // THere is a shutdownhook being registered which FB does not see
public final class DefaultExecutor {

  public static final ExecutorService INSTANCE;

  static {
    final int coreThreads = Integer.getInteger("spf4j.executors.defaultExecutor.coreThreads", 0);
    final int maxIdleMillis = Integer.getInteger("spf4j.executors.defaultExecutor.maxIdleMillis", 60000);
    final boolean isDaemon = Boolean.getBoolean("spf4j.executors.defaultExecutor.daemon");
    final String impParam = "spf4j.executors.defaultExecutor.implementation";
    final String value = System.getProperty(impParam, "spf4j");
    switch (value) {
      case "spf4j":
        LifoThreadPoolExecutorSQP lifoExec = new LifoThreadPoolExecutorSQP("defaultExecutor", coreThreads,
                Integer.MAX_VALUE, maxIdleMillis, 0, isDaemon);
        lifoExec.exportJmx();
        INSTANCE = lifoExec;
        break;
      case "fjp": // EXPERIMENTAL! canceling with interrupt a future of taks submited does not seem to work!
        INSTANCE = new ForkJoinPool(32767);
        break;
      case "legacy":
        INSTANCE = new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, maxIdleMillis, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new CustomThreadFactory("DefaultExecutor", isDaemon));
        break;
      default:
        throw new IllegalArgumentException("Ivalid setting for " + impParam + " = " + value);
    }
    org.spf4j.base.Runtime.queueHookAtEnd(new AbstractRunnable(true) {

      @Override
      public void doRun() throws InterruptedException {
        INSTANCE.shutdown();
        INSTANCE.awaitTermination(org.spf4j.base.Runtime.WAIT_FOR_SHUTDOWN_NANOS, TimeUnit.NANOSECONDS);
        List<Runnable> remaining = INSTANCE.shutdownNow();
        if (remaining.size() > 0) {
          org.spf4j.base.Runtime.error("Remaining tasks: " + remaining);
        }
      }
    });
  }

  private DefaultExecutor() {
  }

  public static ExecutorService instance() {
    return INSTANCE;
  }

}
