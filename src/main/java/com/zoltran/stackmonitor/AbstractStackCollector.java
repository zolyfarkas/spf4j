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
    private SampleNode samples;

    private int nrNodes = 0;
    
    @Override
    public SampleNode applyOnSamples(Function<SampleNode, SampleNode> predicate) {
        synchronized (sampleSync) {
            return samples = predicate.apply(samples);
        }
    }


    @Override
    public void clear() {
        synchronized (sampleSync) {
            samples = null;
            nrNodes = 0;
        }
    }
    
    protected void addSample(StackTraceElement[] stackTrace) {
        synchronized (sampleSync) {
            if (samples == null) {
                samples = new SampleNode(stackTrace, stackTrace.length - 1);
                nrNodes += stackTrace.length +1;
            } else {
                nrNodes += samples.addSample(stackTrace, stackTrace.length - 1);
            }
        }
    }

    @Override
    public String toString() {
        synchronized (sampleSync) {
            return "AbstractStackCollector{" + "samples=" + samples + ", nrNodes=" + nrNodes + '}';
        }
    }

    public int getNrNodes() {
        return nrNodes;
    }

    
}
