/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.stackmonitor;

import com.google.common.base.Function;

/**
 *
 * @author zoly
 */
public interface StackCollector {

    SampleNode applyOnCpuSamples(Function<SampleNode, SampleNode> predicate);

    SampleNode applyOnWaitSamples(Function<SampleNode, SampleNode> predicate);

    void clear();

    void sample();
    
}
