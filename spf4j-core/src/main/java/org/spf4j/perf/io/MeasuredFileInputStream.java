
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.spf4j.perf.MeasurementRecorderSource;

/**
 *
 * @author zoly
 */
@CleanupObligation
@SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
public final class MeasuredFileInputStream extends FileInputStream {

    private final Class<?> from;
    private final File file;
    private final MeasurementRecorderSource recorderSource;

    public MeasuredFileInputStream(final String name, final Class<?> from,
            final MeasurementRecorderSource recorderSource)
            throws FileNotFoundException {
        this(new File(name), from, recorderSource);
    }

    public MeasuredFileInputStream(final File file, final Class<?> from, final MeasurementRecorderSource recorderSource)
            throws FileNotFoundException {
        super(file);
        this.from = from;
        this.recorderSource = recorderSource;
        this.file = file;
    }

    public MeasuredFileInputStream(final FileDescriptor fdObj, final Class<?> from,
            final MeasurementRecorderSource recorderSource) {
        super(fdObj);
        this.from = from;
        this.recorderSource = recorderSource;
        this.file = null;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        int result = super.read(b);
        recorderSource.getRecorder(from).record(result);
        return result;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        int result = super.read(b, off, len);
        recorderSource.getRecorder(from).record(result);
        return result;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result >= 0) {
            recorderSource.getRecorder(from).record(1);
        }
        return result;
    }

    @Override
    public String toString() {
        return "MeasuredFileInputStream{" + "from=" + from + ", file=" + file + ", recorderSource="
                + recorderSource + '}';
    }

}
