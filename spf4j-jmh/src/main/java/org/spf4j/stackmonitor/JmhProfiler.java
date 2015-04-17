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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.spf4j.stackmonitor.proto.Converter;

/**
 *
 * @author zoly
 */
public final class JmhProfiler implements InternalProfiler {

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

    @Override
    public void beforeIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams) {
        SAMPLER.start();
    }

    @Override
    public Collection<? extends Result> afterIteration(final BenchmarkParams benchmarkParams,
            final IterationParams iterationParams) {
        try {
            SAMPLER.stop();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException ex) {
            throw new RuntimeException(ex);
        }
        SampleNode collected = SAMPLER.getStackCollector().clear();
        try {
            return Arrays.asList(new StackResult(collected, benchmarkParams.id(), true));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean checkSupport(final List<String> msgs) {
        return true;
    }

    @Override
    public String label() {
        return "spf4j";
    }

    @Override
    public String getDescription() {
        return "spf4j stack sampler";
    }

    public static final class StackResult extends Result<StackResult> {

        private static final long serialVersionUID = 1L;

        private final SampleNode samples;
        private final String benchmark;
        private final String fileName;


        public StackResult(final SampleNode samples, final String benchmark, final boolean isIteration)
        throws IOException {
            super(ResultRole.SECONDARY, "@stack", of(Double.NaN), "---", AggregationPolicy.AVG);
            this.samples = samples;
            this.benchmark = benchmark;
            if (isIteration) {
                this.fileName = null;
            } else {
                this.fileName = DUMP_FOLDER + "/" + benchmark + ".ssdump";
                Converter.saveToFile(fileName, samples);
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

        @Override
        public String extendedInfo(final String label) {
            if (fileName == null) {
                return samples.toString();
            } else {
                return "Stack sampling profile at: " + fileName;
            }
        }


    }

    public static final class StackAggregator implements Aggregator<StackResult> {

        @Override
        public StackResult aggregate(final Collection<StackResult> results) {
            if (results.isEmpty()) {
                throw new IllegalArgumentException("Nothig to aggregate: " + results);
            }
            Iterator<StackResult> it = results.iterator();
            StackResult result = it.next();
            SampleNode agg = result.getSamples();
            final String benchmark = result.getBenchmark();
            while (it.hasNext()) {
                result = it.next();
                String obenchmark = result.getBenchmark();
                if (!benchmark.equals(obenchmark)) {
                    throw new RuntimeException("Should not aggregate " + benchmark + " with " + obenchmark);
                }
                agg = SampleNode.aggregate(agg, result.getSamples());
            }
            try {
                return new StackResult(agg, benchmark, false);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

}
