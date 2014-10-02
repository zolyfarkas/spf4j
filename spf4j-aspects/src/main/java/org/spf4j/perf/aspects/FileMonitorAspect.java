
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.spf4j.perf.io.MeasuredFileInputStream;
import org.spf4j.perf.io.MeasuredFileOutputStream;
import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.impl.RecorderFactory;

/**
 * Aspect that intercepts File Read and File Write.
 * @author zoly
 */
@Aspect
@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
public final class FileMonitorAspect {
    
    public static final int SAMPLE_TIME = Integer.parseInt(System.getProperty("perf.file.sampleTimeMillis", "300000"));

    private static final MeasurementRecorderSource RECORDER_READ =
            RecorderFactory.createScalableCountingRecorderSource("file-read", "bytes",
            SAMPLE_TIME);
    private static final MeasurementRecorderSource RECORDER_WRITE =
            RecorderFactory.createScalableCountingRecorderSource("file-write", "bytes",
            SAMPLE_TIME);

    @Around("call(long java.nio.channels.FileChannel.read(..))")
    public Object nioReadLong(final ProceedingJoinPoint pjp) throws Throwable {
        Long result = (Long) pjp.proceed();
        if (result >= 0) {
            RECORDER_READ.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }

    @Around("call(int java.nio.channels.FileChannel.read(..))")
    public Object nioReadInt(final ProceedingJoinPoint pjp) throws Throwable {
        Integer result = (Integer) pjp.proceed();
        if (result >= 0) {
            RECORDER_READ.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }

    @Around("call(long java.nio.channels.FileChannel.write(..))")
    public Object nioWriteLong(final ProceedingJoinPoint pjp) throws Throwable {
        Long result = (Long) pjp.proceed();
        if (result >= 0) {
            RECORDER_WRITE.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }

    @Around("call(int java.nio.channels.FileChannel.write(..))")
    public Object nioWriteInt(final ProceedingJoinPoint pjp) throws Throwable {
        Integer result = (Integer) pjp.proceed();
        RECORDER_WRITE.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        return result;
    }

    @Around("call(java.io.FileInputStream.new(java.io.File))")
    public Object fileIS(final ProceedingJoinPoint pjp) throws Throwable {
        pjp.proceed();
        return new MeasuredFileInputStream((File) pjp.getArgs()[0],
                pjp.getSourceLocation().getWithinType(), RECORDER_READ);
    }

    @Around("call(java.io.FileOutputStream.new(java.io.File))")
    public Object fileOS(final ProceedingJoinPoint pjp) throws Throwable {
        pjp.proceed();
        return new MeasuredFileOutputStream((File) pjp.getArgs()[0],
                pjp.getSourceLocation().getWithinType(), RECORDER_WRITE);
    }
}
