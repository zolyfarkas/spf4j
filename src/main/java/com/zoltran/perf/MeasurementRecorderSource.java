/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf;

/**
 *
 * @author zoly
 */
public interface MeasurementRecorderSource {
    
    MeasurementRecorder getRecorder(Object forWhat);
    
}
