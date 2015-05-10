
package org.spf4j.perf.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.perf.MeasurementAccumulator;

/**
 *
 * @author zoly
 */
public abstract class AbstractMeasurementAccumulator implements MeasurementAccumulator {

    private long minTime;
    private long maxTime;

    public AbstractMeasurementAccumulator() {
        minTime = Long.MAX_VALUE;
        maxTime = 0;
    }

    @Override
    public final void recordAt(final long timestampMillis, final long measurement) {
        this.record(measurement);
        if (minTime >  timestampMillis) {
            minTime = timestampMillis;
        }
        if (timestampMillis > maxTime) {
            maxTime = timestampMillis;
        }
    }

    public final long getMinTime() {
        return minTime;
    }

    public final long getMaxTime() {
        return maxTime;
    }

    @Override
    @SuppressFBWarnings
    public void close() {
        // Default do nothing;
    }

}
