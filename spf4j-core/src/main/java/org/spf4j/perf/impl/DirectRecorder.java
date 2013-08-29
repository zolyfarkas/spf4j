/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.perf.impl;

import java.io.IOException;
import org.spf4j.perf.EntityMeasurementsInfo;
import org.spf4j.perf.MeasurementDatabase;
import org.spf4j.perf.MeasurementRecorder;

/**
 *
 * @author zoly
 */
public final class DirectRecorder implements MeasurementRecorder {
    
    private final EntityMeasurementsInfo info;
    private static final String[] MEASUREMENTS = {"value"};
    private final MeasurementDatabase database;

    
    public DirectRecorder(final Object measuredEntity, final String unitOfMeasurement,
            final MeasurementDatabase database) {
        this.info = new EntityMeasurementsInfoImpl(measuredEntity, unitOfMeasurement,
                MEASUREMENTS, new String[]{unitOfMeasurement});
        this.database = database;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    public void record(final long measurement) {
        try {
            database.saveMeasurements(info, new long [] {measurement}, System.currentTimeMillis(), 0);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
