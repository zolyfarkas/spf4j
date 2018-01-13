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
package org.spf4j.base;

import java.io.IOException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class ArraysBenchmark {

  @State(Scope.Thread)
  public static class ThreadState {

    private final String[] testArray = new String[10000];

    {
      java.util.Arrays.fill(testArray, "a");
    }
  }

  @Benchmark
  public void testSpf4jFillVeryLarge(final ThreadState ts) throws IOException {
    org.spf4j.base.Arrays.fill(ts.testArray, 0, 10000, null);
  }

  @Benchmark
  public void testjdkFillVeryLarge(final ThreadState ts) throws IOException {
    java.util.Arrays.fill(ts.testArray, 0, 10000, null);
  }

  @Benchmark
  public void testSpf4jFillLarge(final ThreadState ts) throws IOException {
    org.spf4j.base.Arrays.fill(ts.testArray, 0, 1000, null);
  }

  @Benchmark
  public void testjdkFillLarge(final ThreadState ts) throws IOException {
    java.util.Arrays.fill(ts.testArray, 0, 1000, null);
  }

  @Benchmark
  public void testSpf4jFillSmall(final ThreadState ts) throws IOException {
    org.spf4j.base.Arrays.fill(ts.testArray, 0, 10, null);
  }

  @Benchmark
  public void testjdkFillSmall(final ThreadState ts) throws IOException {
    java.util.Arrays.fill(ts.testArray, 0, 10, null);
  }

  @Benchmark
  public void testSpf4jFillMedium(final ThreadState ts) throws IOException {
    org.spf4j.base.Arrays.fill(ts.testArray, 0, 100, null);
  }

  @Benchmark
  public void testjdkFillMedium(final ThreadState ts) throws IOException {
    java.util.Arrays.fill(ts.testArray, 0, 100, null);
  }

}
