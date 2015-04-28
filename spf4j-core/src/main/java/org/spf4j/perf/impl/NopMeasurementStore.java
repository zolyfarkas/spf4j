
package org.spf4j.perf.impl;

import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementStore;

/**
 *
 * @author zoly
 */
public final class NopMeasurementStore implements MeasurementStore {

    @Override
    public void alocateMeasurements(final EntityMeasurementsInfo measurement, final int sampleTimeMillis) {
        // Do nothing
    }

    @Override
    public void saveMeasurements(final EntityMeasurementsInfo measurementInfo, final long timeStampMillis,
            final int sampleTimeMillis, final long... measurements) {
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
