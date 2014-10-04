
package org.spf4j.perf.impl;

import org.spf4j.perf.MeasurementProcessor;

/**
 *
 * @author zoly
 */
public abstract class MeasurementAggregator implements MeasurementProcessor {

    private long minTime;
    private long maxTime;
    
    public MeasurementAggregator() {
        minTime = 0;
        maxTime = 0;
    }
    
    @Override
    public final void record(final long measurement, final long timestampMillis) {
        this.record(measurement);
        if (minTime < timestampMillis) {
            minTime = maxTime;
        } else if (timestampMillis > maxTime) {
            maxTime = timestampMillis;
        }
    }

    public final long getMinTime() {
        return minTime;
    }

    public final long getMaxTime() {
        return maxTime;
    }
    
}
