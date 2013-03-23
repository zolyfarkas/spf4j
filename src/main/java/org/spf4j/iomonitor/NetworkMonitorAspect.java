
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
package org.spf4j.iomonitor;

import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.RecorderFactory;
import java.io.InputStream;
import java.io.OutputStream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 *
 * @author zoly
 */
@Aspect
public class NetworkMonitorAspect {

    private static final MeasurementRecorderSource RECORDER_READ =
            RecorderFactory.createScalableCountingRecorderSource("network-read", "bytes",
            Integer.valueOf(System.getProperty("perf.network.sampleTime", "300000")));
    
    private static final MeasurementRecorderSource RECORDER_WRITE =
            RecorderFactory.createScalableCountingRecorderSource("network-write", "bytes",
            Integer.valueOf(System.getProperty("perf.network.sampleTime", "300000")));
    

    @Around("call(long java.nio.channels.SocketChannel.read(..))")
    public Object nioReadLong(ProceedingJoinPoint pjp) throws Throwable {
        Long result = (Long) pjp.proceed();
        if (result >=0 ) {
            RECORDER_READ.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }
    
    @Around("call(int java.nio.channels.SocketChannel.read(..))")
    public Object nioReadInt(ProceedingJoinPoint pjp) throws Throwable {
        Integer result = (Integer) pjp.proceed();
        if (result >=0) {
            RECORDER_READ.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }
    
    @Around("call(long java.nio.channels.SocketChannel.write(..))")
    public Object nioWriteLong(ProceedingJoinPoint pjp) throws Throwable {
        Long result = (Long) pjp.proceed();
        if (result >= 0) {
            RECORDER_WRITE.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }
    
    @Around("call(int java.nio.channels.SocketChannel.write(..))")
    public Object nioWriteInt(ProceedingJoinPoint pjp) throws Throwable {
        Integer result = (Integer) pjp.proceed();
        if (result >= 0) {
            RECORDER_WRITE.getRecorder(pjp.getSourceLocation().getWithinType()).record(result);
        }
        return result;
    }
    
    
    @Around("call(* java.net.Socket.getInputStream())")
    public Object socketIS(ProceedingJoinPoint pjp) throws Throwable {
        InputStream result = (InputStream) pjp.proceed();
        return new MeasuredInputStream(result, pjp.getSourceLocation().getWithinType(), RECORDER_READ);
    }
    
    @Around("call(* java.net.Socket.getOutputStream())")
    public Object socketOS(ProceedingJoinPoint pjp) throws Throwable {
        OutputStream result = (OutputStream) pjp.proceed();
        return new MeasuredOutputStream(result, pjp.getSourceLocation().getWithinType(), RECORDER_WRITE);
    }
    
    
}
