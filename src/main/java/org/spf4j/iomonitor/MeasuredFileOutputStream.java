/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.iomonitor;

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
public final class MeasuredFileOutputStream extends FileOutputStream {

    private final Class<?> from;
    private final MeasurementRecorderSource recorderSource;

    public MeasuredFileOutputStream(final String name, final Class<?> from,
            final MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        super(name);
        this.from = from;
        this.recorderSource = recorderSource;
    }

    public MeasuredFileOutputStream(final String name, final boolean append,
            final Class<?> from, final MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        super(name, append);
        this.from = from;
        this.recorderSource = recorderSource;
    }

    public MeasuredFileOutputStream(final File file, final Class<?> from,
            final MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        super(file);
        this.from = from;
        this.recorderSource = recorderSource;
    }

    public MeasuredFileOutputStream(final File file, final boolean append, final Class<?> from,
            final MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        super(file, append);
        this.from = from;
        this.recorderSource = recorderSource;
    }

    public MeasuredFileOutputStream(final FileDescriptor fdObj, final Class<?> from,
            final MeasurementRecorderSource recorderSource) {
        super(fdObj);
        this.from = from;
        this.recorderSource = recorderSource;
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
}
