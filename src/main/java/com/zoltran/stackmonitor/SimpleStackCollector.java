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
        synchronized (sampleSync) {
            for (Map.Entry<Thread, StackTraceElement[]> entry : stackDump.entrySet()) {
                StackTraceElement[] stackTrace = entry.getValue();
                if (stackTrace.length > 0 && !entry.getKey().equals(Thread.currentThread())) {
                    Method m = new Method(stackTrace[0]);
                    if (MethodClassifier.isWaitMethod(m)) {
                        if (waitSamples == null) {
                            waitSamples = new SampleNode(stackTrace, stackTrace.length - 1);
                        } else {
                            waitSamples.addSample(stackTrace, stackTrace.length - 1);
                        }

                    }  else {
                        if (cpuSamples == null) {
                            cpuSamples = new SampleNode(stackTrace, stackTrace.length - 1);
                        } else {
                            cpuSamples.addSample(stackTrace, stackTrace.length - 1);
                        }

                    }
                }
            }
        }
    }


    
}
