package org.spf4j.perf.impl;

import java.io.IOException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import junit.framework.Assert;
import org.junit.Test;
import org.spf4j.jmx.Client;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;


/**
 *
 * @author zoly
 */
public class RecorderFactoryTest {


    @Test
    public void testRecorderFactory() throws InterruptedException, IOException,
            InstanceNotFoundException, MBeanException, AttributeNotFoundException, ReflectionException {
        MeasurementRecorder rec = RecorderFactory.createScalableQuantizedRecorder(RecorderFactoryTest.class,
                "ms", 100000000, 10, 0, 6, 10);
        rec.record(1);
        int sum = 1;
        for (int i = 0; i < 10; i++) {
            rec.record(i);
            sum += i;
        }
       String ret3 = (String) Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "org.spf4j.perf.recorders", "class_" + RecorderFactoryTest.class.getName(), "measurementsAsString");
       System.out.println(ret3);
       Assert.assertTrue(ret3.contains("" + sum + "," + 11));
    }

    private static final class RsTest {

    }


    @Test
    public void testRecorderFactoryDyna() throws InterruptedException, IOException,
            InstanceNotFoundException, MBeanException, AttributeNotFoundException, ReflectionException {
        MeasurementRecorderSource rec = RecorderFactory.createScalableQuantizedRecorderSource(RsTest.class,
                "ms", 100000000, 10, 0, 6, 10);
        rec.getRecorder("test").record(1);
        int sum = 1;
        for (int i = 0; i < 10; i++) {
            rec.getRecorder("test").record(i);
            sum += i;
        }
       String ret3 = (String) Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "org.spf4j.perf.recorders", "class_" + RecorderFactoryTest.class.getName() + "_RsTest",
                "measurementsAsString");
       System.out.println(ret3);
       Assert.assertTrue(ret3.contains("test," + sum + "," + 11));
    }


}
