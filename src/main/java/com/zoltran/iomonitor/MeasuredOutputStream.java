
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
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author zoly
 */
public class MeasuredOutputStream extends OutputStream {
    
    private final OutputStream os;
    private final Class<?> from;
    private final MeasurementRecorderSource recorderSource;
    
    
    public MeasuredOutputStream(OutputStream os, Class<?> from, MeasurementRecorderSource recorderSource) {
     this.os = os;
     this.from = from;
     this.recorderSource = recorderSource;
    }

    @Override
    public void write(int b) throws IOException {
        os.write(b);
        recorderSource.getRecorder(from).record(1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        os.write(b);
        recorderSource.getRecorder(from).record(b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
        recorderSource.getRecorder(from).record(len);
    }

    @Override
    public void flush() throws IOException {
        os.flush();
    }

    @Override
    public void close() throws IOException {
        os.close();
    }
  
} 
