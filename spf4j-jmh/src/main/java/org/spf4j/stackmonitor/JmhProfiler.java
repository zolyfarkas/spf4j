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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.spf4j.ssdump2.Converter;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public final class JmhProfiler implements InternalProfiler {

  /**
   * Sampling period
   */
  private static final int SAMPLE_PERIOD_MSEC = Integer.getInteger("jmh.stack.period", 10);

  private static final String DUMP_FOLDER = System.getProperty("jmh.stack.profiles", org.spf4j.base.Runtime.USER_DIR);

  private static final Sampler SAMPLER = new Sampler(SAMPLE_PERIOD_MSEC, Integer.MAX_VALUE,
          (t) -> new FastStackCollector(false, true, new Thread[]{t}));

  private static volatile String benchmarkName;

  public static Sampler getStackSampler() {
    return SAMPLER;
  }

  public static String benchmarkName() {
    return benchmarkName;
  }

  @Override
  @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
  public void beforeIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams) {
    benchmarkName = benchmarkParams.id();
    SAMPLER.start();
  }

  @Override
  @Nonnull
  public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams,
          final IterationParams iterationParams, final IterationResult ir) {
    try {
      SAMPLER.stop();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return Collections.EMPTY_LIST;
    }
    Map<String, SampleNode> c = SAMPLER.getStackCollectionsAndReset();
    if (c.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    SampleNode collected = c.values().iterator().next();
    try {
      return Collections.singletonList(new StackResult(collected, benchmarkParams.id(), true));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String getDescription() {
    return "spf4j stack sampler";
  }

  public static final class StackResult extends Result<StackResult> {

    private static final long serialVersionUID = 1L;

    private final SampleNode samples;
    private final String benchmark;

    public StackResult(final SampleNode samples, final String benchmark, final boolean isIteration)
            throws IOException {
      super(ResultRole.SECONDARY, "@stack", of(Double.NaN), "---", AggregationPolicy.AVG);
      this.samples = samples;
      this.benchmark = benchmark;
      if (!isIteration) {
        String fileName = DUMP_FOLDER + '/' + benchmark + ".ssdump2";
        Converter.save(new File(fileName), samples);
      }
    }

    public SampleNode getSamples() {
      return samples;
    }

    public String getBenchmark() {
      return benchmark;
    }

    @Override
    protected Aggregator<StackResult> getThreadAggregator() {
      return new StackAggregator();
    }

    @Override
    protected Aggregator<StackResult> getIterationAggregator() {
      return new StackAggregator();
    }

    @Override
    public String toString() {
      return "<delayed till summary>";
    }

  }

  public static final class StackAggregator implements Aggregator<StackResult> {

    @Override
    public StackResult aggregate(final Collection<StackResult> results) {
      if (results.isEmpty()) {
        throw new IllegalArgumentException("Nothig to aggregate: " + results);
      } else if (results.size() == 1) {
        return results.iterator().next();
      }
      Iterator<StackResult> it = results.iterator();
      StackResult result = it.next();
      SampleNode agg = result.getSamples();
      final String benchmark = result.getBenchmark();
      while (it.hasNext()) {
        result = it.next();
        String obenchmark = result.getBenchmark();
        if (!benchmark.equals(obenchmark)) {
          throw new Spf4jProfilerException("Should not aggregate " + benchmark + " with " + obenchmark);
        }
        agg = SampleNode.aggregate(agg, result.getSamples());
      }
      try {
        return new StackResult(agg, benchmark, false);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

  }

}
