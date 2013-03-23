/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.iomonitor;

import org.spf4j.perf.MeasurementRecorderSource;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author zoly
 */
public class MeasuredFileInputStream extends FileInputStream {

    private final Class<?> from;
    private final MeasurementRecorderSource recorderSource;

    public MeasuredFileInputStream(String name, Class<?> from, MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        super(name);
        this.from = from;
        this.recorderSource = recorderSource;
    }

    public MeasuredFileInputStream(File file, Class<?> from, MeasurementRecorderSource recorderSource) throws FileNotFoundException {
        super(file);
        this.from = from;
        this.recorderSource = recorderSource;
    }

    public MeasuredFileInputStream(FileDescriptor fdObj, Class<?> from, MeasurementRecorderSource recorderSource) {
        super(fdObj);
        this.from = from;
        this.recorderSource = recorderSource;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = super.read(b);
        recorderSource.getRecorder(from).record(result);
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
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
}
