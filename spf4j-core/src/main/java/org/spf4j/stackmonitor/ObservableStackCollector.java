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
package org.spf4j.stackmonitor;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;

@Beta
public final class ObservableStackCollector extends AbstractStackCollector {

  private final Predicate<Thread> threadFilter;

  private final ConcurrentMap<Thread, Consumer<StackTraceElement[]>> stConsumers;

  private Thread[] requestFor = new Thread[]{};

  public ObservableStackCollector(final boolean collectForMain, final String... xtraIgnoredThreads) {
    this(FastStackCollector.createNameBasedFilter(false, collectForMain, xtraIgnoredThreads));
  }

  public ObservableStackCollector(final Predicate<Thread> threadFilter) {
    this.threadFilter = threadFilter;
    this.stConsumers = new ConcurrentHashMap<>();
  }

  @Nullable
  public Consumer<StackTraceElement[]> registerConsumer(final Thread thread,
          final Consumer<StackTraceElement[]> consumer) {
    return stConsumers.put(thread, consumer);
  }

  public Consumer<StackTraceElement[]> unregisterConsumer(final Thread thread) {
    return stConsumers.remove(thread);
  }

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void sample(final Thread ignore) {
    Thread[] threads = FastStackCollector.getThreads();
    final int nrThreads = threads.length;
    if (requestFor.length < nrThreads) {
      requestFor = new Thread[nrThreads - 1];
    }
    int j = 0;
    for (int i = 0; i < nrThreads; i++) {
      Thread th = threads[i];
      if (ignore != th && !threadFilter.test(th)) { // not interested in these traces
        requestFor[j++] = th;
      }
    }
    Arrays.fill(requestFor, j, requestFor.length, null);
    StackTraceElement[][] stackDump = FastStackCollector.getStackTraces(requestFor);
    for (int i = 0; i < j; i++) {
      Thread t = requestFor[i];
      StackTraceElement[] stackTrace = stackDump[i];
      Consumer<StackTraceElement[]> consumer = this.stConsumers.get(t);
      if (consumer != null) {
        consumer.accept(stackTrace);
      }
      if (stackTrace != null && stackTrace.length > 0) {
        addSample(stackTrace);
      } else {
        addSample(new StackTraceElement[]{
          new StackTraceElement("Thread", t.getName(), "", 0)
        });
      }
    }
  }

}
