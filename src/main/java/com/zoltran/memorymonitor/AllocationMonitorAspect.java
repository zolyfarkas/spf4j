/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.memorymonitor;

import com.zoltran.perf.MeasurementRecorderSource;
import com.zoltran.perf.RecorderFactory;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly    
 */
@Aspect
public class AllocationMonitorAspect {

    private static final MeasurementRecorderSource RECORDER = 
            RecorderFactory.createScalableCountingRecorderSource("allocations", "instances", 1000);
    
    private static final Logger LOG = LoggerFactory.getLogger(AllocationMonitorAspect.class);
    
    @AfterReturning(pointcut = "call(*+.new(..)) && this(from)", returning = "obj", argNames="from,obj" )
    public void afterAllocation(Object from, Object obj) {
        LOG.info("Object {} allocated  from  {}", obj, from );
        RECORDER.getRecorder(from).record(1);
    }
}
