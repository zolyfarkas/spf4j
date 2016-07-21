/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.stackmonitor;

import org.spf4j.base.Method;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class SampleNodeTest {

    public SampleNodeTest() {
    }

    @Test
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public void testSampleNode() {

        System.out.println("sample");
        StackTraceElement[] st1 = new StackTraceElement[3];
        st1[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        st1[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
        st1[2] = new StackTraceElement("C1", "m3", "C1.java", 12);
        SampleNode node1 = SampleNode.createSampleNode(st1);
        SampleNode node2 = new SampleNode(st1, st1.length - 1);
        System.out.println(node1);
        System.out.println(node2);

        Assert.assertEquals(4, node1.getNrNodes());
        Assert.assertEquals(4, node2.getNrNodes());


        StackTraceElement[] st2 = new StackTraceElement[1];
        st2[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        SampleNode.addToSampleNode(node1, st2);
        node2.addSample(st2, st2.length - 1);
        System.out.println(node1);
        System.out.println(node2);
        Assert.assertEquals(5, node1.getNrNodes());
        Assert.assertEquals(5, node2.getNrNodes());

        StackTraceElement[] st3 = new StackTraceElement[3];
        st3[0] = new StackTraceElement("C2", "m1", "C2.java", 10);
        st3[1] = new StackTraceElement("C2", "m2", "C2.java", 11);
        st3[2] = new StackTraceElement("C2", "m3", "C2.java", 12);
        SampleNode.addToSampleNode(node1, st3);
        node2.addSample(st3, st3.length - 1);
        System.out.println(node1);
        System.out.println(node2);

        StackTraceElement[] st4 = new StackTraceElement[3];
        st4[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        st4[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
        st4[2] = new StackTraceElement("C1", "m4", "C1.java", 14);
        SampleNode.addToSampleNode(node1, st4);
        node2.addSample(st4, st4.length - 1);

        SampleNode.addToSampleNode(node1, st1);
        node2.addSample(st1, st1.length - 1);

        System.out.println("n1 = " + node1);
        System.out.println("n2 = " + node2);
        Assert.assertEquals(node1.toString(), node2.toString());

        SampleNode agg = SampleNode.aggregate(node1, node2);
        System.out.println("n1 + n2 = " + agg);
        Assert.assertEquals(node1.getSampleCount() + node2.getSampleCount(), agg.getSampleCount());
        final Method method = Method.getMethod("C1", "m3");
        Assert.assertEquals(node1.getSubNodes().get(method).getSampleCount()
                + node2.getSubNodes().get(method).getSampleCount(),
                agg.getSubNodes().get(method).getSampleCount());

    }
}