/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.iomonitor;

import com.zoltran.perf.MeasurementRecorderSource;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 *
 * @author zoly
 */
public class MeasuredFileWriter extends OutputStreamWriter {

    
    public MeasuredFileWriter(String fileName, Class<?> from, MeasurementRecorderSource recorderSource) throws IOException {
	super(new MeasuredOutputStream(new FileOutputStream(fileName), from, recorderSource));
    }

    public MeasuredFileWriter(String fileName, boolean append,  Class<?> from, MeasurementRecorderSource recorderSource) throws IOException {
	super(new MeasuredOutputStream(new FileOutputStream(fileName, append), from, recorderSource));
    }

    public MeasuredFileWriter(File file, Class<?> from, MeasurementRecorderSource recorderSource) throws IOException {
	super(new MeasuredOutputStream(new FileOutputStream(file), from, recorderSource));
    }

    public MeasuredFileWriter(File file, boolean append, Class<?> from, MeasurementRecorderSource recorderSource) throws IOException {
        super(new MeasuredOutputStream(new FileOutputStream(file, append), from, recorderSource));
    }

    public MeasuredFileWriter(FileDescriptor fd, Class<?> from, MeasurementRecorderSource recorderSource) {
	super(new MeasuredOutputStream(new FileOutputStream(fd), from, recorderSource));
    }

}
