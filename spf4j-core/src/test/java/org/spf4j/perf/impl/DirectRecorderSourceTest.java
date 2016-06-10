
package org.spf4j.perf.impl;

import java.io.IOException;
import org.junit.Test;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;

/**
 *
 * @author zoly
 */
public final class DirectRecorderSourceTest {


    @Test
    public void testDirectRecorder() throws IOException {
        MeasurementRecorderSource recorderSource =
                RecorderFactory.createDirectRecorderSource("test", "description");
        MeasurementRecorder recorder = recorderSource.getRecorder("A");
        long time = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            recorder.recordAt(time + (long) i * 1000, i);
        }
        MeasurementRecorder recorder2 = recorderSource.getRecorder("B");
        for (int i = 0; i < 100; i++) {
            recorder2.recordAt(time + (long) i * 1000, i);
        }
        recorderSource.close();
        org.spf4j.perf.RecorderFactoryTest.assertData("(test,A)", 4950);
        org.spf4j.perf.RecorderFactoryTest.assertData("(test,B)", 4950);
    }

}
