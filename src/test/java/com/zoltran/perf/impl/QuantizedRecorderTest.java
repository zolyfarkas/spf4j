/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf.impl;

import java.util.Map;
import static org.junit.Assert.assertArrayEquals;
import org.junit.*;

/**
 *
 * @author zoly
 */
public class QuantizedRecorderTest {
    
    public QuantizedRecorderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of record method, of class QuantizedRecorder.
     */
    @Test
    public void testRecord1() {
        System.out.println("record");
        QuantizedRecorder instance = new QuantizedRecorder("test", "ms", 
                10, 0, 3, 10);
        instance.record(0);
        instance.record(1);
        instance.record(2);
        instance.record(10);
        instance.record(11);
        instance.record(250);
        instance.record(15000);        
        System.out.println(instance);
    }

        @Test
    public void testRecord2() {
        System.out.println("record");
        QuantizedRecorder instance = new QuantizedRecorder("test", "ms", 
                10, -3, 3, 10);
        instance.record(0);
        instance.record(1);
        instance.record(2);
        instance.record(10);
        instance.record(11);
        instance.record(250);
        instance.record(15000);
        
        System.out.println(instance);
    }
    
    @Test
    public void testRecord3() {
        System.out.println("record");
        QuantizedRecorder instance = new QuantizedRecorder("test", "ms", 
                10, 0, 1, 10);
        instance.record(0);
        instance.record(1);
        instance.record(2);
        instance.record(10);
        instance.record(11);
        instance.record(250);
        instance.record(15000);
        assertArrayEquals(new long[] {0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 4}, instance.getQuatizedMeasurements());
        System.out.println(instance);
        Map result = instance.getMeasurements(false);
        System.out.println(result);
    }
  
    
        @Test
    public void testRecord4() {
        System.out.println("record");
        QuantizedRecorder instance = new QuantizedRecorder("test", "ms", 
                10, -1, 1, 10);
        instance.record(0);
        instance.record(1);
        instance.record(2);
        instance.record(10);
        instance.record(11);
        instance.record(250);
        instance.record(15000);
        instance.record(-15000);
        instance.record(-10);
        instance.record(-10);
        assertArrayEquals(new long[] {1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 4}, instance.getQuatizedMeasurements());
        System.out.println(instance);
        Map result = instance.getMeasurements(false);
        System.out.println(result);
    }


}
