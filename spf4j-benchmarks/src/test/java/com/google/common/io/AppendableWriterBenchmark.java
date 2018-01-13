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
package com.google.common.io;

import java.io.IOException;
import java.io.Writer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.spf4j.base.IntMath;

/**
 *
 * @author zoly
 */
@Fork(2)
@Threads(value = 8)
public class AppendableWriterBenchmark {

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    private final char[] testChars;

    {
      StringBuilder builder = new StringBuilder(4096);
      for (int i = 0; i < 4096; i++) { // simulating a 8k buffer.
        builder.append('A');
      }
      testChars = builder.toString().toCharArray();
    }

  }

  @State(Scope.Thread)
  public static class ThreadState {
    private final StringBuilder sb = new StringBuilder(100000);
    private final IntMath.XorShift32 random = new IntMath.XorShift32();
  }

  @Benchmark
  public final StringBuilder guavaAppendable(final BenchmarkState bs, final ThreadState ts) throws IOException {
    StringBuilder stringBuilder = ts.sb;
    stringBuilder.setLength(0);
    Writer writer = new AppendableWriter(stringBuilder);
    for (int i = 0; i < 25; i++) {
      writer.write(bs.testChars, 0, Math.abs(ts.random.nextInt()) % 4096);
    }
    writer.close();
    return stringBuilder;
  }

  @Benchmark
  public final StringBuilder spf4jAppendable(final BenchmarkState bs, final ThreadState ts) throws IOException {
    StringBuilder stringBuilder = ts.sb;
    stringBuilder.setLength(0);
    Writer writer = new org.spf4j.io.AppendableWriter(stringBuilder);
    for (int i = 0; i < 25; i++) {
      writer.write(bs.testChars, 0, Math.abs(ts.random.nextInt()) % 4096);
    }
    writer.close();
    return stringBuilder;
  }

}
