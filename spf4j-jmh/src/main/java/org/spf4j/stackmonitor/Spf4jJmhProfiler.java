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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.runner.IterationType;
import org.spf4j.concurrent.AtomicSequence;
import org.spf4j.concurrent.UIDGenerator;
import org.spf4j.ssdump2.Converter;

/**
 * Improved implementation of the spf4j profiler to get extra profiling
 * data granularity to be able to corelate to iteration number.
 *
 * @author zoly
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public final class Spf4jJmhProfiler implements InternalProfiler {

  /**
   * Sampling period
   */
  private static final int SAMPLE_PERIOD_MSEC = Integer.getInteger("jmh.stack.period", 10);

  private static final String DUMP_FOLDER = System.getProperty("jmh.stack.profiles", org.spf4j.base.Runtime.USER_DIR);

  private static final Sampler SAMPLER = new Sampler(SAMPLE_PERIOD_MSEC, Integer.MAX_VALUE,
          (t) -> new FastStackCollector(false, true, new Thread[] {t}));

  private static final AtomicInteger MEASUREMENT_ITERATION_COUNTER = new AtomicInteger(1);

  private static final AtomicInteger WARMUP_ITERATION_COUNTER = new AtomicInteger(1);

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
    SampleNode collected;
    if (c.isEmpty()) {
      collected = null;
    } else {
      collected = c.values().iterator().next();
    }
    String iterationId;
    IterationType itType = iterationParams.getType();
    switch (itType) {
      case MEASUREMENT:
        iterationId = "m" + MEASUREMENT_ITERATION_COUNTER.getAndIncrement();
        break;
      case WARMUP:
        iterationId = "w" + WARMUP_ITERATION_COUNTER.getAndIncrement();
        break;
      default:
        throw new IllegalStateException("Unknown type of iteration " + itType);
    }
    return Collections.singletonList(new StackResult(collected, benchmarkParams.id(), iterationId));
  }

  @Override
  public String getDescription() {
    return "spf4j stack sampler";
  }

  public static final class StackResult extends Result<StackResult> {

    private static final long serialVersionUID = 1L;

    private final String benchmark;

    private final File perfDataFile;

    private final String id;

    public StackResult(@Nullable final SampleNode samples, final String benchmark, final String iterationId) {
      super(ResultRole.SECONDARY, "@stack", of(Double.NaN), "---", AggregationPolicy.AVG);
      this.id = iterationId;
      this.benchmark = benchmark;
      if (samples != null) {
        File file = new File(DUMP_FOLDER + '/' + benchmark + '_' + iterationId + ".ssdump2");
        try {
          Converter.save(file, samples);
          this.perfDataFile = file;
        } catch (IOException ex) {
          throw new Spf4jProfilerException("Failed to persist profile data to " + file, ex);
        }
      } else {
        this.perfDataFile = null;
      }
    }

    public String getId() {
      return id;
    }

    @Nullable
    public SampleNode getSamples() throws IOException {
      if (this.perfDataFile == null) {
        return null;
      } else {
        return Converter.load(perfDataFile);
      }
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

    private static final  AtomicSequence SEQ = new AtomicSequence(0);

    @Override
    public StackResult aggregate(final Collection<StackResult> results) {
      if (results.isEmpty()) {
        throw new IllegalArgumentException("Nothig to aggregate: " + results);
      }
      Iterator<StackResult> it = results.iterator();
      if (results.size() == 1) {
        return it.next();
      }
      StackResult result = it.next();
      StringBuilder aggId = new StringBuilder();
      aggId.append("aggregation_");
      SampleNode agg;
      try {
        agg = result.getSamples();
        aggId.append(result.getId());
        final String benchmark = result.getBenchmark();
        while (it.hasNext()) {
          result = it.next();
          String obenchmark = result.getBenchmark();
          if (!benchmark.equals(obenchmark)) {
            throw new UnsupportedOperationException("Should not aggregate " + benchmark + " with " + obenchmark);
          }
          SampleNode sss = result.getSamples();
          if (agg == null) {
            agg = sss;
          } else if (sss != null) {
            agg = SampleNode.aggregate(agg, sss);
          }
          aggId.append('_').append(result.getId());
        }
        truncateId(aggId, 100);
        return new StackResult(agg, benchmark, aggId.toString());
      } catch (IOException ex) {
        throw new Spf4jProfilerException("Failed to aggregate " + results, ex);
      }
    }

    private static void truncateId(final StringBuilder aggId, final int size) {
      int length = aggId.length();
      if (length > size) {
        CharSequence id = new UIDGenerator(SEQ).next();
        aggId.setLength(length - id.length() - 1);
        aggId.append('_');
        aggId.append(id);
      }
    }

  }

}
