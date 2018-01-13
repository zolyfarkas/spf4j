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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class CacheBenchmark {

  private static final CacheLoader<String, String> TEST_LOADER
          = new CacheLoader<String, String>() {

    @Override
    public String load(final String key) throws Exception {
      return "TEST";
    }
  };

  private static final LoadingCache<String, String> GUAVA = CacheBuilder.newBuilder()
          .initialCapacity(16)
          .concurrencyLevel(16)
          .build(TEST_LOADER);
  private static final LoadingCache<String, String> SPF4J = new UnboundedLoadingCache<>(16, 16, TEST_LOADER);

  private static final LoadingCache<String, String> SPF4J2 = new UnboundedLoadingCache2<>(16, 16, TEST_LOADER);

  private static final LoadingCache<String, String> SPF4J_RACY
          = new UnboundedRacyLoadingCache<>(16, 16, TEST_LOADER);

  @Benchmark
  public final String spf4jCache() {
    return SPF4J.getUnchecked("key" + (ThreadLocalRandom.current().nextInt(100)));
  }

  @Benchmark
  public final String spf4j2Cache() {
    return SPF4J2.getUnchecked("key" + (ThreadLocalRandom.current().nextInt(100)));
  }

  @Benchmark
  public final String spf4jRacyCache() {
    return SPF4J_RACY.getUnchecked("key" + (ThreadLocalRandom.current().nextInt(100)));
  }

  @Benchmark
  public final String guavaCache() {
    return GUAVA.getUnchecked("key" + (ThreadLocalRandom.current().nextInt(100)));
  }

}
