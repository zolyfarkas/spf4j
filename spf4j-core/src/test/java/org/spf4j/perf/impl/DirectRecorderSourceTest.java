
package org.spf4j.perf.impl;

import org.junit.Test;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;

/**
 *
 * @author zoly
 */
public final class DirectRecorderSourceTest {
    

    @Test
    public void testSomeMethod() throws InterruptedException {
        MeasurementRecorderSource recorderSource =
                RecorderFactory.createDirectRecorderSource("test", "description");
        MeasurementRecorder recorder = recorderSource.getRecorder("A");
        long time = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            recorder.record(i, time + i * 1000);
        }
        MeasurementRecorder recorder2 = recorderSource.getRecorder("B");
        for (int i = 0; i < 100; i++) {
            recorder2.record(i, time + i * 1000);
        }
    }
    
}
