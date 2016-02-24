package org.spf4j.perf.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import org.spf4j.base.Arrays;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MultiMeasurementRecorder;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("VO_VOLATILE_REFERENCE_TO_ARRAY")
public final class DirectStoreMultiAccumulator implements MultiMeasurementRecorder, Closeable {

    private final MeasurementsInfo info;
    private final MeasurementStore measurementStore;
    private final long tableId;

    private volatile long[] lastRecorded;



    public DirectStoreMultiAccumulator(final MeasurementsInfo info, final MeasurementStore measurementStore) {
        try {
            this.info = info;
            this.measurementStore = measurementStore;
            this.tableId = measurementStore.alocateMeasurements(info, 0);
            this.lastRecorded = Arrays.EMPTY_LONG_ARRAY;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }



    @Override
    public void record(final long... measurement) {
        recordAt(System.currentTimeMillis(), measurement);
    }

    @Override
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "EXS_EXCEPTION_SOFTENING_NO_CHECKED" })
    public void recordAt(final long timestampMillis, final long... measurement) {
        try {
            measurementStore.saveMeasurements(tableId, timestampMillis, measurement);
        } catch (IOException ex) {
           throw new RuntimeException(ex);
        } finally {
            lastRecorded = measurement;
        }
    }

    public void registerJmx() {
        Registry.export("org.spf4j.perf.recorders", info.getMeasuredEntity().toString(), this);
    }

    @JmxExport
    public long[] getLastRecorded() {
        return lastRecorded.clone();
    }

    @JmxExport
    public String getInfo() {
        return info.toString();
    }

    @Override
    public void close() {
        Registry.unregister("org.spf4j.perf.recorders", info.getMeasuredEntity().toString());
    }

}
