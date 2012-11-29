/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.stackmonitor;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author zoly
 */
public class SimpleStackCollectorTest {
    

    /**
     * Test of sample method, of class SimpleStackCollector.
     */
    @Test
    public void testSample() {
        System.out.println("sample");
        SimpleStackCollector instance = new SimpleStackCollector();
        StackTraceElement [] st1 = new StackTraceElement[3];
        st1[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        st1[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
        st1[2] = new StackTraceElement("C1", "m3", "C1.java", 12);
        instance.addSample(st1);
        System.out.println(instance);
        assertEquals(4, instance.getNrNodes());
        
        StackTraceElement [] st2 = new StackTraceElement[1];
        st2[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        instance.addSample(st2);
        System.out.println(instance);
        assertEquals(5, instance.getNrNodes());
        
        StackTraceElement [] st3 = new StackTraceElement[3];
        st3[0] = new StackTraceElement("C2", "m1", "C2.java", 10);
        st3[1] = new StackTraceElement("C2", "m2", "C2.java", 11);
        st3[2] = new StackTraceElement("C2", "m3", "C2.java", 12);
        instance.addSample(st3);
        System.out.println(instance);
        
        StackTraceElement [] st4 = new StackTraceElement[3];
        st4[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
        st4[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
        st4[2] = new StackTraceElement("C1", "m4", "C1.java", 14);
        instance.addSample(st4);
        
        System.out.println(instance);
        
    }
}
