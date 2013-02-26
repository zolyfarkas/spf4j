/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.memorymonitor;

import com.google.common.base.Strings;

/**
 *
 * @author zoly
 */
public class TestClass {

    public static void testAllocInStaticContext() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            System.err.println("TS" + i + Strings.repeat("A", i % 2 * (int) (Math.random() * 10)));
            if (i % 100 == 0) {
                Thread.sleep(500);
            }
        }
    }
    
}
