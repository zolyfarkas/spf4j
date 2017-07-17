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
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.spf4j.base.Version;

/**
 *
 * @author zoly
 */
public final class JmhFlightRecorderProfiler implements ExternalProfiler {

    private static final String DUMP_FOLDER = System.getProperty("jmh.stack.profiles", org.spf4j.base.Runtime.USER_DIR);

    private static final String DEFAULT_OPTIONS = System.getProperty("jmh.fr.options",
            "defaultrecording=true,settings=profile");

    private static volatile String benchmarkName;

    private volatile String dumpFile;

    @Override
    public Collection<String> addJVMInvokeOptions(final BenchmarkParams params) {
        return Collections.emptyList();
    }

    public static String benchmarkName() {
        return benchmarkName;
    }


    /**
     * See:
     * http://docs.oracle.com/cd/E15289_01/doc.40/e15070/usingjfr.htm
     * and
     * http://docs.oracle.com/cd/E15289_01/doc.40/e15070/config_rec_data.htm
     * @param params
     * @return
     */
    @Override
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public Collection<String> addJVMOptions(final BenchmarkParams params) {
        final String id = params.id();
        benchmarkName = id;
        dumpFile = DUMP_FOLDER + '/' + id + ".jfr";
        String flightRecorderOptions = DEFAULT_OPTIONS + ",dumponexit=true,dumponexitpath=" + dumpFile;
        return Arrays.asList(
                "-XX:+FlightRecorder",
                "-XX:FlightRecorderOptions=" + flightRecorderOptions);
    }

    @Override
    public void beforeTrial(final BenchmarkParams benchmarkParams) {
        final List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        if (new Version(org.spf4j.base.Runtime.JAVA_VERSION).compareTo(new Version("1.8.0_40")) <= 0
                && !inputArguments.contains("-XX:+UnlockCommercialFeatures")) {
            throw new Spf4jProfilerException("-XX:+UnlockCommercialFeatures must pre present in the JVM options,"
                    + " current options are: " + inputArguments);
        }
    }


    @Override
    public boolean allowPrintOut() {
        return true;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }


    @Override
    public String getDescription() {
        return "Java Flight Recording profiler runs for every benchmark.";
    }

    @Override
    public Collection<? extends Result> afterTrial(final BenchmarkResult bp, final long l,
            final File file, final File file1) {
        NoResult r = new NoResult("Profile saved to " + dumpFile + ", results: " + bp
            + ", stdOutFile = " + file + ", stdErrFile = " + file1);
        return Collections.singleton(r);
    }

    private static final class NoResult extends Result<NoResult> {
        private static final long serialVersionUID = 1L;

        private final String output;

        NoResult(final String output) {
            super(ResultRole.SECONDARY, "JFR", of(Double.NaN), "N/A", AggregationPolicy.SUM);
            this.output = output;
        }

        @Override
        protected Aggregator<NoResult> getThreadAggregator() {
            return new NoResultAggregator();
        }

        @Override
        protected Aggregator<NoResult> getIterationAggregator() {
            return new NoResultAggregator();
        }

        private static class NoResultAggregator implements Aggregator<NoResult> {

            @Override
            public NoResult aggregate(final Collection<NoResult> results) {
                StringBuilder agg = new StringBuilder();
                for (NoResult r : results) {
                    agg.append(r.output);
                }
                return new NoResult(agg.toString());
            }
        }
    }

    @Override
    public String toString() {
        return "JmhFlightRecorderProfiler{" + "dumpFile=" + dumpFile + '}';
    }

}
