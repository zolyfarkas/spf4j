/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.memorymonitor;

import com.zoltran.perf.RecorderFactory;
import java.io.IOException;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class AllocationMonitorAspectTest {
    
    public AllocationMonitorAspectTest() {
    }
    
    private long startTime = System.currentTimeMillis();

    private static void  testAllocInStaticContext () throws InterruptedException {
        for(int i=0; i< 1000; i++) {
            System.out.println("S"+i);
            if (i%100 == 0) {
                Thread.sleep(500);
            }
        }
    }
    
    /**
     * Test of afterAllocation method, of class AllocationMonitorAspect.
     */
    @Test
    public void testAfterAllocation() throws InterruptedException, IOException {
        for(int i=0; i< 1000; i++) {
            System.out.println("T"+i);
            if (i%100 == 0) {
                Thread.sleep(500);
            }
        }
        testAllocInStaticContext();
        
        System.out.println(RecorderFactory.TS_DATABASE.generateCharts(startTime, System.currentTimeMillis(), 1200, 600));
    }
}
