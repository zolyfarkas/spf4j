package org.spf4j.stackmonitor;

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
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;

/**
 *
 * @author zoly
 */
public final class JmhFlightRecorderProfiler implements ExternalProfiler {

    private static final String DUMP_FOLDER = System.getProperty("jmh.stack.profiles", org.spf4j.base.Runtime.USER_DIR);

    private static final String DEFAULT_OPTIONS = System.getProperty("jmh.fr.options",
            "defaultrecording=true,settings=profile");
    
    
    /**
     * Holds whether recording is supported (checking the existence of the needed unlocking flag)
     */
    private static final boolean IS_SUPPORTED;

    static {
        IS_SUPPORTED = ManagementFactory.getRuntimeMXBean().getInputArguments()
                .contains("-XX:+UnlockCommercialFeatures");
    }

    @Override
    public Collection<String> addJVMInvokeOptions(final BenchmarkParams params) {
        return Collections.emptyList();
    }

    private volatile String dumpFile;

    /**
     * See:
     * http://docs.oracle.com/cd/E15289_01/doc.40/e15070/usingjfr.htm
     * and
     * http://docs.oracle.com/cd/E15289_01/doc.40/e15070/config_rec_data.htm
     * @param params
     * @return
     */
    @Override
    public Collection<String> addJVMOptions(final BenchmarkParams params) {
        dumpFile = DUMP_FOLDER + "/" + params.id() + ".jfr";
        String flightRecorderOptions = DEFAULT_OPTIONS + ",dumponexit=true,dumponexitpath=" + dumpFile;
        return Arrays.asList(
                "-XX:+FlightRecorder",
                "-XX:FlightRecorderOptions=" + flightRecorderOptions);
    }

    @Override
    public void beforeTrial(final BenchmarkParams benchmarkParams) {
    }

    @Override
    public Collection<? extends Result> afterTrial(final BenchmarkParams benchmarkParams,
            final File stdOut, final File stdErr) {
        NoResult r = new NoResult("Profile saved to " + dumpFile);
        return Collections.singleton(r);
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
    public boolean checkSupport(final List<String> msgs) {
        msgs.add("Commercial features of the JVM need to be enabled for this profiler.");
        return IS_SUPPORTED;
    }

    @Override
    public String label() {
        return "jfr";
    }

    @Override
    public String getDescription() {
        return "Java Flight Recording profiler runs for every benchmark.";
    }

    private static final class NoResult extends Result<NoResult> {
        private static final long serialVersionUID = 1L;

        private final String output;

        public NoResult(final String output) {
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

        @Override
        public String extendedInfo(final String label) {
            return "JFR Messages:\n--------------------------------------------\n" + output;
        }

        private static class NoResultAggregator implements Aggregator<NoResult> {

            @Override
            public Result aggregate(final Collection<NoResult> results) {
                StringBuilder agg = new StringBuilder();
                for (NoResult r : results) {
                    agg.append(r.output);
                }
                return new NoResult(agg.toString());
            }
        }
    }
}
