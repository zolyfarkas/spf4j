
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
import java.io.InputStream;

/**
 *
 * @author zoly
 */
public class MeasuredInputStream  extends InputStream{
    private final InputStream is;
    private final Class<?> from;
    private final MeasurementRecorderSource recorderSource;

    public MeasuredInputStream(InputStream is, Class<?> from, MeasurementRecorderSource recorderSource) {
        this.is = is;
        this.from = from;
        this.recorderSource = recorderSource;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = is.read(b);
        recorderSource.getRecorder(from).record(result);
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = is.read(b, off, len);
        recorderSource.getRecorder(from).record(result);
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        int result = is.read();
        recorderSource.getRecorder(from).record(1);
        return result;
    }
    
    
    
    
}
