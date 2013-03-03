/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.iomonitor;

import com.zoltran.perf.MeasurementRecorderSource;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

/**
 *
 * @author zoly
 */
public class MeasuredFileReader extends InputStreamReader {


    public MeasuredFileReader(String fileName, Class<?> from, MeasurementRecorderSource recorderSource) throws FileNotFoundException {
	super(new MeasuredInputStream( new FileInputStream(fileName), from, recorderSource));
    }


    public MeasuredFileReader(File file, Class<?> from, MeasurementRecorderSource recorderSource) throws FileNotFoundException {
	super(new MeasuredInputStream(new FileInputStream(file), from, recorderSource));
    }

    public MeasuredFileReader(FileDescriptor fd, Class<?> from, MeasurementRecorderSource recorderSource) {
	super(new MeasuredInputStream(new FileInputStream(fd), from, recorderSource));
    }

}
