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

import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.concurrent.NotThreadSafe;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.Threads;

/**
 * A stack sample collector that collects samples only for code executed within a execution context.
 * Plus this collector will attach the samples to the contexts.
 *
 * This context requires ProfiledExecutionContextFactory wrapper.
 *
 * @author Zoltan Farkas
 */
@NotThreadSafe
public final class TracingExecutionContextStackCollector implements ISampler {

  private final Supplier<Iterable<Map.Entry<Thread, ExecutionContext>>> execCtxSupplier;

  private Thread[] requestFor;

  private ExecutionContext[] contexts;

  private final TMap<String, StackCollector> collections;

  public TracingExecutionContextStackCollector(
          final Supplier<Iterable<Map.Entry<Thread, ExecutionContext>>> execCtxSupplier) {
    this(100, execCtxSupplier);
  }

  public TracingExecutionContextStackCollector(final int maxNrThreads,
          final Supplier<Iterable<Map.Entry<Thread, ExecutionContext>>> execCtxSupplier) {
    requestFor = new Thread[maxNrThreads];
    contexts = new ExecutionContext[maxNrThreads];
    this.execCtxSupplier = execCtxSupplier;
    collections = new THashMap<>();
  }

  @Override
  public void sample() {
    Iterable<Map.Entry<Thread, ExecutionContext>> currentThreads = execCtxSupplier.get();
    int i = 0;
    for (Map.Entry<Thread, ExecutionContext> entry : currentThreads) {
      requestFor[i] = entry.getKey();
      contexts[i++] = entry.getValue();
      if (i >= requestFor.length) {
        break;
      }
    }
    Arrays.fill(requestFor, i, requestFor.length, null);
    StackTraceElement[][] stackTraces = Threads.getStackTraces(requestFor);
    for (int j = 0; j < i; j++) {
      StackTraceElement[] stackTrace = stackTraces[j];
      ExecutionContext context = contexts[j];
      String name = context.getName();
      StackCollector c = collections.computeIfAbsent(name, (k) -> new StackCollectorImpl());
      if (stackTrace != null && stackTrace.length > 0) {
        c.collect(stackTrace);
        context.compute("TSS", (String k, SampleNode v) -> {
          if (v == null) {
            return SampleNode.createSampleNode(stackTrace);
          } else {
            SampleNode.addToSampleNode(v, stackTrace);
            return v;
          }
        });
      } else {
        c.collect(new StackTraceElement[]{
          new StackTraceElement("Thread", requestFor[j].getName(), "", 0)
        });
      }
    }
  }

  @Override
  public Map<String, SampleNode> getCollectionsAndReset() {
    TMap<String, SampleNode> result = new THashMap<>(collections.size());
    collections.forEachEntry((k, v) -> {
      result.put(k, v.getAndReset());
      return true;
    });
    return result;
  }

  @Override
  public Map<String, SampleNode> getCollections() {
    TMap<String, SampleNode> result = new THashMap<>(collections.size());
    collections.forEachEntry((k, v) -> {
      result.put(k, v.get());
      return true;
    });
    return result;
  }

  @Override
  public String toString() {
    return "TracingExecutionContextStackCollector{" + "execCtxSupplier=" + execCtxSupplier
            + ", collections=" + collections + '}';
  }



}
