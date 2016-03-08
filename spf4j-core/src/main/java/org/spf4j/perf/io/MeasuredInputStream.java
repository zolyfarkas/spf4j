
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
package org.spf4j.perf.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import java.io.IOException;
import java.io.InputStream;
import org.spf4j.perf.MeasurementRecorderSource;

/**
 *
 * @author zoly
 */
@CleanupObligation
public final class MeasuredInputStream  extends InputStream {
    private final InputStream is;
    private final String from;
    private final MeasurementRecorderSource recorderSource;

    public MeasuredInputStream(final InputStream is, final String from,
            final MeasurementRecorderSource recorderSource) {
        this.is = is;
        this.from = from;
        this.recorderSource = recorderSource;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        int result = is.read(b);
        recorderSource.getRecorder(from).record(result);
        return result;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        int result = is.read(b, off, len);
        recorderSource.getRecorder(from).record(result);
        return result;
    }

    @Override
    public long skip(final long n) throws IOException {
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
    public void mark(final int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        int result = is.read();
        if (result >= 0) {
            recorderSource.getRecorder(from).record(1);
        }
        return result;
    }

    @Override
    public String toString() {
        return "MeasuredInputStream{" + "is=" + is + ", from=" + from + ", recorderSource=" + recorderSource + '}';
    }
       
}
