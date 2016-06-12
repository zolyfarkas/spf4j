
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
package org.spf4j.perf.aspects;

import org.spf4j.base.InstrumentationHelper;
import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.impl.RecorderFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.spf4j.base.MutableInteger;
import org.spf4j.stackmonitor.StackTrace;

/**
 * Aspect that intercepts all memory allocations in your code.
 * where and how much has been allocated is stored in a tsdb database.
 this class needs to remain object allocation free to work!
 this aspect will recordAt details about every x allocations done in a particular thread...
 *
 * @author zoly
 */
@Aspect
public final class SamplingAllocationMonitorAspect {

   private static final MeasurementRecorderSource RECORDER;

   private static final int SAMPLE_COUNT = Integer.getInteger("spf4j.perf.allocations.sampleCount", 100);

   static {
       RECORDER = RecorderFactory.createScalableCountingRecorderSource("allocations", "bytes",
               AllocationMonitorAspect.SAMPLE_TIME_MILLIS);

   }

    @AfterReturning(pointcut = "call(*.new(..))", returning = "obj", argNames = "jp,obj")
    public void afterAllocation(final JoinPoint jp, final Object obj) {
        MutableInteger counter = Counter.SAMPLING_COUNTER.get();
        int value = counter.getValue();
        if (value < SAMPLE_COUNT) {
            counter.setValue(value + 1);
        } else {
            // the stack trace get and the object size method are expensive to be done at every allocation...
            counter.setValue(0);
            StackTrace st = StackTrace.from(Thread.currentThread().getStackTrace(), 2);
            RECORDER.getRecorder(st).record(InstrumentationHelper.getObjectSize(obj));
        }
    }
}
