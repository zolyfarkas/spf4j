
package org.spf4j.perf.impl;

import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;

/**
 *
 * @author zoly
 */
public final class NopMeasurementStore implements MeasurementStore {

    @Override
    public long alocateMeasurements(final MeasurementsInfo measurement, final int sampleTimeMillis) {
        // Do nothing
        return -1;
    }

    @Override
    public void saveMeasurements(final long tableId, final long timeStampMillis, final long... measurements) {
        // Do nothing
    }

    @Override
    public void flush() {
        // Do nothing
    }

    @Override
    public void close() {
        // Do nothing
    }

}
