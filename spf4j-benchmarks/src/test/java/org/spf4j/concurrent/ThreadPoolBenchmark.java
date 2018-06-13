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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@Fork(2)
@Threads(value = 8)
public class ThreadPoolBenchmark {

  @State(Scope.Benchmark)
  public static class LazySpf {

    private final ExecutorService es = LifoThreadPoolBuilder.newBuilder()
            .withQueueSizeLimit(10000)
            .withCoreSize(8)
            .withMaxSize(8).build();

    @TearDown
    public void close() {
      es.shutdown();
      DefaultExecutor.INSTANCE.shutdown();
    }

  }



  public static long testPool(final ExecutorService executor)
          throws InterruptedException, IOException, ExecutionException {
    final java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
    final int testCount = 1000;
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        adder.increment();
      }
    };
    Future[] futures = new Future[testCount];
    for (int i = 0; i < testCount; i++) {
      futures[i] = executor.submit(runnable);
    }
    for (Future<?> future : futures) {
      future.get();
    }
    long longValue = adder.longValue();
    if (longValue != (long) testCount) {
      throw new RuntimeException("Something is wrong with thread pool " + longValue);
    }
    return longValue;
  }

  @Benchmark
  public final long spfLifoTpBenchmark(final LazySpf exec)
          throws InterruptedException, IOException, ExecutionException {
    return testPool(exec.es);
  }


}
