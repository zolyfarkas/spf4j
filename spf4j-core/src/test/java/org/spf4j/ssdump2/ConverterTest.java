package org.spf4j.ssdump2;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Handler;
import org.spf4j.ssdump2.avro.ASample;
import org.spf4j.stackmonitor.Method;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author zoly
 */
public class ConverterTest {

    private SampleNode testSample() {
         StackTraceElement[] st1 = new StackTraceElement[3];
        st1[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        st1[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
        st1[2] = new StackTraceElement("C1", "m3", "C1.java", 12);
        SampleNode node = SampleNode.createSampleNode(st1);

        StackTraceElement[] st2 = new StackTraceElement[1];
        st2[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        SampleNode.addToSampleNode(node, st2);

        StackTraceElement[] st3 = new StackTraceElement[3];
        st3[0] = new StackTraceElement("C2", "m1", "C2.java", 10);
        st3[1] = new StackTraceElement("C2", "m2", "C2.java", 11);
        st3[2] = new StackTraceElement("C2", "m3", "C2.java", 12);
        SampleNode.addToSampleNode(node, st3);

        StackTraceElement[] st4 = new StackTraceElement[3];
        st4[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        st4[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
        st4[2] = new StackTraceElement("C1", "m4", "C1.java", 14);
        SampleNode.addToSampleNode(node, st4);
        return node;
    }


    @Test
    public void test() {
        SampleNode testSample = testSample();
        final List<ASample> samples = new ArrayList<>();
        Converter.convert(Method.ROOT, testSample, -1, 0, new Handler<ASample, RuntimeException>() {

            @Override
            public void handle(final ASample object, final long deadline) {
                samples.add(object);
            }
        });
        SampleNode back = Converter.convert(samples.iterator());
        System.out.println(back);
        Assert.assertEquals(testSample.toString(), back.toString());
    }

}
