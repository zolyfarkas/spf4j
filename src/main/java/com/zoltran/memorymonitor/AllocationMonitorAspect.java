/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.memorymonitor;

import com.zoltran.base.InstrumentationHelper;
import com.zoltran.perf.MeasurementRecorderSource;
import com.zoltran.perf.RecorderFactory;
import org.aspectj.lang.JoinPoint;
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
            RecorderFactory.createScalableCountingRecorderSource("allocations", "instances", 
            Integer.valueOf(System.getProperty("perf.allocations.sampleTime", "1000")) );
    
    private static final boolean RECORD_OBJECT_SIZE = Boolean.valueOf(System.getProperty("perf.allocations.recordSize", "true"));
    
    private static final Logger LOG = LoggerFactory.getLogger(AllocationMonitorAspect.class);
    
    @AfterReturning(pointcut = "call(*.new(..))", returning = "obj", argNames="jp,obj" )
    public void afterAllocation(JoinPoint jp, Object obj) {
        if (RECORD_OBJECT_SIZE) {
            RECORDER.getRecorder(jp.getSourceLocation().getWithinType()).record(InstrumentationHelper.getObjectSize(obj));
        } else {
            RECORDER.getRecorder(jp.getSourceLocation().getWithinType()).record(1);
        }
    }
}
