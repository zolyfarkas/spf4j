
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.spf4j.perf.MeasurementRecorderSource;

/**
 *
 * @author zoly
 */
@CleanupObligation
@SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
public final class MeasuredFileOutputStream extends FileOutputStream {

    private final Class<?> from;
    private final MeasurementRecorderSource recorderSource;
    private final File file;

    public MeasuredFileOutputStream(final String name, final Class<?> from,
            final MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        this(new File(name), from, recorderSource);
    }

    public MeasuredFileOutputStream(final String name, final boolean append,
            final Class<?> from, final MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        this(new File(name), append, from, recorderSource);
    }

    public MeasuredFileOutputStream(final File file, final Class<?> from,
            final MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        this(file, false, from, recorderSource);
    }

    public MeasuredFileOutputStream(final File file, final boolean append, final Class<?> from,
            final MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        super(file, append);
        this.from = from;
        this.recorderSource = recorderSource;
        this.file = file;
    }

    public MeasuredFileOutputStream(final FileDescriptor fdObj, final Class<?> from,
            final MeasurementRecorderSource recorderSource) {
        super(fdObj);
        this.from = from;
        this.recorderSource = recorderSource;
        this.file = null;
    }

    @Override
    public void write(final int b) throws IOException {
        super.write(b);
        recorderSource.getRecorder(from).record(1);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        super.write(b);
        recorderSource.getRecorder(from).record(b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        super.write(b, off, len);
        recorderSource.getRecorder(from).record(len);
    }

    @Override
    public String toString() {
        return "MeasuredFileOutputStream{" + "from=" + from + ", recorderSource=" + recorderSource
                + ", file=" + file + '}';
    }



}
