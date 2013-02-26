/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.base;

import java.lang.instrument.Instrumentation;

/**
 *
 * @author zoly
 */
public final  class InstrumentationHelper {
    
    
    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    public static long getObjectSize(Object o) {
        return instrumentation.getObjectSize(o);
    }
}
