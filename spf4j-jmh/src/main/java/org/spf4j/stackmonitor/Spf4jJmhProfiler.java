/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.stackmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
public final class Spf4jJmhProfiler implements InternalProfiler {

  /**
   * Sampling period
   */
  private static final int SAMPLE_PERIOD_MSEC = Integer.getInteger("jmh.stack.period", 10);

  private static final String DUMP_FOLDER = System.getProperty("jmh.stack.profiles", org.spf4j.base.Runtime.USER_DIR);

  private static final Sampler SAMPLER = new Sampler(SAMPLE_PERIOD_MSEC, Integer.MAX_VALUE,
          new FastStackCollector(true));

  public static Sampler getStackSampler() {
    return SAMPLER;
  }

  private static volatile String benchmarkName;

  public static String benchmarkName() {
    return benchmarkName;
  }

  @Override
  @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
  public void beforeIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams) {
    benchmarkName = benchmarkParams.id();
    SAMPLER.start();
  }

  private static final AtomicInteger MEASUREMENT_ITERATION_COUNTER = new AtomicInteger(1);

  private static final AtomicInteger WARMUP_ITERATION_COUNTER = new AtomicInteger(1);

  @Override
  public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams,
          final IterationParams iterationParams, final IterationResult ir) {
    try {
      SAMPLER.stop();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    } catch (ExecutionException | TimeoutException ex) {
      throw new RuntimeException(ex);
    }
    SampleNode collected = SAMPLER.getStackCollector().clear();
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
    try {
      return Arrays.asList(new StackResult(collected, benchmarkParams.id(), iterationId));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
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

    public StackResult(@Nullable final SampleNode samples, final String benchmark, final String iterationId)
            throws IOException {
      super(ResultRole.SECONDARY, "@stack", of(Double.NaN), "---", AggregationPolicy.AVG);
      this.id = iterationId;
      this.benchmark = benchmark;
      if (samples != null) {
        this.perfDataFile = new File(DUMP_FOLDER + '/' + benchmark + '_' + iterationId + ".ssdump2");
        Converter.save(this.perfDataFile, samples);
      } else {
        this.perfDataFile = null;
      }
    }

    public String getId() {
      return id;
    }

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

  private static final  AtomicSequence SEQ = new AtomicSequence(0);

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
      StringBuilder aggId = new StringBuilder();
      aggId.append("aggregation_");
      SampleNode agg;
      try {
        agg = result.getSamples();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      aggId.append(result.getId());
      final String benchmark = result.getBenchmark();
      while (it.hasNext()) {
        result = it.next();
        String obenchmark = result.getBenchmark();
        if (!benchmark.equals(obenchmark)) {
          throw new RuntimeException("Should not aggregate " + benchmark + " with " + obenchmark);
        }
        try {
          SampleNode sss = result.getSamples();
          if (agg == null) {
            agg = sss;
          } else if (sss != null) {
            agg = SampleNode.aggregate(agg, sss);
          }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        aggId.append('_').append(result.getId());
      }
      int length = aggId.length();
      if (length > 100) {
        CharSequence id = new UIDGenerator(SEQ).next();
        aggId.setLength(length - id.length() - 1);
        aggId.append('_');
        aggId.append(id);
      }
      try {
        return new StackResult(agg, benchmark, aggId.toString());
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

  }

}
