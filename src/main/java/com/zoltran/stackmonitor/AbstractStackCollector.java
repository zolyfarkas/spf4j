/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.stackmonitor;

import com.google.common.base.Function;
import javax.annotation.concurrent.GuardedBy;

/**
 *
 * @author zoly
 */
public abstract class AbstractStackCollector implements StackCollector {
 
    
    protected final Object sampleSync = new Object();
    @GuardedBy(value = "sampleSync")
    protected SampleNode cpuSamples;
    @GuardedBy(value = "sampleSync")
    protected SampleNode waitSamples;
    
        @Override
    public SampleNode applyOnCpuSamples(Function<SampleNode, SampleNode> predicate) {
        synchronized (sampleSync) {
            return cpuSamples = predicate.apply(cpuSamples);
        }
    }

    @Override
    public SampleNode applyOnWaitSamples(Function<SampleNode, SampleNode> predicate) {
        synchronized (sampleSync) {
            return waitSamples = predicate.apply(waitSamples);
        }
    }

    @Override
    public void clear() {
        synchronized (sampleSync) {
            cpuSamples = null;
            waitSamples = null;
        }
    }
    
    
}
