
/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.zoltran.memorymonitor;

import com.zoltran.base.InstrumentationHelper;
import com.zoltran.perf.MeasurementRecorderSource;
import com.zoltran.perf.RecorderFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

/**
 *
 * @author zoly    
 */
@Aspect
public class AllocationMonitorAspect {

    private static final MeasurementRecorderSource RECORDER = 
            RecorderFactory.createScalableCountingRecorderSource("allocations", "instances", 
            Integer.valueOf(System.getProperty("perf.allocations.sampleTime", "300000")) );
    
    private static final boolean RECORD_OBJECT_SIZE = Boolean.valueOf(System.getProperty("perf.allocations.recordSize", "true"));
       
    @AfterReturning(pointcut = "call(*.new(..))", returning = "obj", argNames="jp,obj" )
    public void afterAllocation(JoinPoint jp, Object obj) {
        if (RECORD_OBJECT_SIZE) {
            RECORDER.getRecorder(jp.getSourceLocation().getWithinType()).record(InstrumentationHelper.getObjectSize(obj));
        } else {
            RECORDER.getRecorder(jp.getSourceLocation().getWithinType()).record(1);
        }
    }
}
