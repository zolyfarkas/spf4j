/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.stackmonitor;

import java.util.Map;

/**
 *
 * @author zoly
 */
public class SimpleStackCollector extends AbstractStackCollector {

    @Override
    public void sample() {
        Map<Thread, StackTraceElement[]> stackDump = Thread.getAllStackTraces();
        recordStackDump(stackDump);
    }

    private void recordStackDump(Map<Thread, StackTraceElement[]> stackDump) {
        for (Map.Entry<Thread, StackTraceElement[]> entry : stackDump.entrySet()) {
            StackTraceElement[] stackTrace = entry.getValue();
            if (stackTrace.length > 0 && !entry.getKey().equals(Thread.currentThread())) {
                addSample(stackTrace);
            }
        }
    }
}
