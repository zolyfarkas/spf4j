/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf;

import com.zoltran.perf.impl.QuantizedRecorder;
import com.zoltran.perf.impl.RRDMeasurementDatabase;
import com.zoltran.perf.impl.ScalableMeasurementRecorder;
import com.zoltran.perf.impl.ScalableMeasurementRecorderSource;

/**
 *
 * @author zoly
 */
public final class RecorderFactory {
    
    
    static final RRDMeasurementDatabase RRD_DATABASE = 
            new RRDMeasurementDatabase(System.getProperty("rrd.perf.folder", 
            System.getProperty("java.io.tmpdir")));
    
    static {
        try
        {
            RRD_DATABASE.registerJmx();
        } catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
    
    public static MeasurementRecorder createScalableQuantizedRecorder(Object forWhat, String unitOfMeasurement, int sampleTimeMillis,
             int factor, int lowerMagnitude, 
            int higherMagnitude, int quantasPerMagnitude ) {
        return new ScalableMeasurementRecorder(new QuantizedRecorder(forWhat,
                unitOfMeasurement, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude), sampleTimeMillis, RRD_DATABASE);
    }
    
    
    public static MeasurementRecorderSource createScalableQuantizedRecorderSource(Object forWhat, String unitOfMeasurement, int sampleTimeMillis,
             int factor, int lowerMagnitude, 
            int higherMagnitude, int quantasPerMagnitude ) {
        return new ScalableMeasurementRecorderSource(new QuantizedRecorder(forWhat,
                unitOfMeasurement, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude), sampleTimeMillis, RRD_DATABASE);
    }
    
}
