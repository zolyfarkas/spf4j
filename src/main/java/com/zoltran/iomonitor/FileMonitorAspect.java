
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
package com.zoltran.iomonitor;

import com.zoltran.perf.MeasurementRecorderSource;
import com.zoltran.perf.RecorderFactory;
import java.io.File;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 *
 * @author zoly
 */
@Aspect
public class FileMonitorAspect {

    private static final MeasurementRecorderSource RECORDER_READ =
            RecorderFactory.createScalableCountingRecorderSource("file-read", "bytes",
            Integer.valueOf(System.getProperty("perf.file.sampleTime", "300000")));
    
    private static final MeasurementRecorderSource RECORDER_WRITE =
            RecorderFactory.createScalableCountingRecorderSource("file-write", "bytes",
            Integer.valueOf(System.getProperty("perf.file.sampleTime", "300000")));
    

    @Around("call(long java.nio.channels.FileChannel.read(..))")
    public Object nioReadLong(ProceedingJoinPoint pjp) throws Throwable {
        Long result = (Long) pjp.proceed();
        if (result >= 0) {
            RECORDER_READ.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }
    
    @Around("call(int java.nio.channels.FileChannel.read(..))")
    public Object nioReadInt(ProceedingJoinPoint pjp) throws Throwable {
        Integer result = (Integer) pjp.proceed();
        if (result >= 0) {
            RECORDER_READ.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }
    
    @Around("call(long java.nio.channels.FileChannel.write(..))")
    public Object nioWriteLong(ProceedingJoinPoint pjp) throws Throwable {
        Long result = (Long) pjp.proceed();
        if (result >= 0) {
            RECORDER_WRITE.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }
    
    @Around("call(int java.nio.channels.FileChannel.write(..))")
    public Object nioWriteInt(ProceedingJoinPoint pjp) throws Throwable {
        Integer result = (Integer) pjp.proceed();
        RECORDER_WRITE.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        return result;
    }
    
    @Around("call(java.io.FileInputStream.new(java.io.File))")
    public Object fileIS(ProceedingJoinPoint pjp) throws Throwable {
        pjp.proceed();
        return new MeasuredFileInputStream((File)pjp.getArgs()[0], pjp.getSourceLocation().getWithinType(), RECORDER_READ);
    }
    
    @Around("call(java.io.FileOutputStream.new(java.io.File))")
    public Object fileOS(ProceedingJoinPoint pjp) throws Throwable {
        pjp.proceed();
        return new MeasuredFileOutputStream((File)pjp.getArgs()[0], pjp.getSourceLocation().getWithinType(), RECORDER_WRITE);
    }
    
    
}
