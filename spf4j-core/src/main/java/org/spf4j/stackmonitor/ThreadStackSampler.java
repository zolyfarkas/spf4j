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

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.concurrent.NotThreadSafe;
import org.spf4j.base.Threads;

/**
 * A stack sample collector that collects samples only for suppllied threads.
 *
 * @author Zoltan Farkas
 */
@NotThreadSafe
public final class ThreadStackSampler implements ISampler {

  private final Supplier<Iterable<Thread>> threadSupplier;

  private final StackCollector collector;

  private Thread[] requestFor;

  public ThreadStackSampler(final Supplier<Iterable<Thread>> threadSupplier) {
    this(20, threadSupplier);
  }

  public ThreadStackSampler(final int maxSampledThreads,
          final Supplier<Iterable<Thread>> threadSupplier) {
    requestFor = new Thread[maxSampledThreads];
    this.threadSupplier = threadSupplier;
    this.collector = new StackCollectorImpl();
  }

  @Override
  public void sample() {
    Iterable<Thread> currentThreads = threadSupplier.get();
    int i = 0;
    for (Thread t : currentThreads) {
      requestFor[i++] = t;
      if (i >= requestFor.length) {
        break;
      }
    }
    Arrays.fill(requestFor, i, requestFor.length, null);
    StackTraceElement[][] stackTraces = Threads.getStackTraces(requestFor);
    for (int j = 0; j < i; j++) {
      StackTraceElement[] stackTrace = stackTraces[j];
      if (stackTrace != null && stackTrace.length > 0) {
        collector.collect(stackTrace);
      } else {
        collector.collect(new StackTraceElement[]{
          new StackTraceElement("Thread", requestFor[j].getName(), "", 0)
        });
      }
    }
  }

  @Override
  public Map<String, SampleNode> getCollectionsAndReset() {
    SampleNode nodes = collector.getAndReset();
    return nodes == null ? Collections.EMPTY_MAP : ImmutableMap.of(threadSupplier.toString(), nodes);
  }

  @Override
  public Map<String, SampleNode> getCollections() {
    SampleNode nodes = collector.get();
    return nodes == null ? Collections.EMPTY_MAP : ImmutableMap.of(threadSupplier.toString(), nodes);
  }

  @Override
  public String toString() {
    return "ThreadStackSampler{" + "threadSupplier=" + threadSupplier + ", collector=" + collector + '}';
  }

}
