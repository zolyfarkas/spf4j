/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.stackmonitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 *
 * @author zoly
 */
public class MxStackCollector extends AbstractStackCollector {

    private final ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();

    @Override
    public void sample() {
        ThreadInfo[] stackDump = threadMX.dumpAllThreads(true, true);
        recordStackDump(stackDump);
    }

    private void recordStackDump(ThreadInfo[] stackDump) {
        for (ThreadInfo entry : stackDump) {
            StackTraceElement[] stackTrace = entry.getStackTrace();
            if (stackTrace.length > 0 && !(entry.getThreadId() == Thread.currentThread().getId())) {
                addSample(stackTrace);
            }
        }
    }
}
