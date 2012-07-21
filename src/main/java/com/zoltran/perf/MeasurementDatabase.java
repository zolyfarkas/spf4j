/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf;

import com.zoltran.base.ReportGenerator;
import java.io.IOException;

/**
 *
 * @author zoly
 */
public interface MeasurementDatabase extends ReportGenerator {
    
    void saveMeasurements(EntityMeasurements measurement, long timeStampMillis, int sampleTimeMillis)
            throws IOException;
    
}
