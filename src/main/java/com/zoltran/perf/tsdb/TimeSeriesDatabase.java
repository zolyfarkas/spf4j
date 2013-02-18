/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.tsdb;

import com.google.common.base.Charsets;
import java.io.Closeable;
import java.io.DataInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Yet another time series database. Why? because all the other ts databases had
 * various constraints that restrict the functionality I can add to spf4j.
 *
 * Features: 1. file will acquire an exclusive lock when open. 3. measurements
 * can be added dynamically anytime to a database. 3. long measurement names.
 *
 * Format:
 *
 * Header { type:char[4], version:int, sampleInterval:int, int metaDataSize,
 * metaData: byte[metaDataSize]}
 *
 * ColumnInfo(colName:varchar(1024), nextColumnInfo:long )
 *
 * DataFragment { startTime:long, Values ... Values }
 *
 * Values {nrValues:short, { values:double[nrValues] | nextDataFragment:long }}
 *
 * @author zoly
 */

public class TimeSeriesDatabase implements Closeable {
    
    public static final int VERSION = 1;
    private final List<ColumnInfo> columns;
    private final RandomAccessFile file;
    private final Header header;
    private final TableOfContents toc;
    private ColumnInfo lastColumnInfo;
    
    public TimeSeriesDatabase(String pathToDatabaseFile, int sampleInterval, byte[] metaData) throws FileNotFoundException, IOException {
        file = new RandomAccessFile(pathToDatabaseFile, "rw");
        // read or create header
        if (file.length() == 0) {
            this.header = new Header(VERSION, sampleInterval, metaData);
            this.header.writeTo(file);
            this.toc = new TableOfContents(file.getFilePointer());
            this.toc.writeTo(file);
        } else {
            this.header = new Header(file);
            this.toc = new TableOfContents(file.getFilePointer());
        }
        columns = new ArrayList<ColumnInfo>();
        if (toc.getFirstColumnInfo() > 0) {
            file.seek(toc.getFirstColumnInfo());
            ColumnInfo colInfo = new ColumnInfo(file);
            columns.add(colInfo);
            lastColumnInfo = colInfo;
            while (colInfo.getNextColumnInfo() > 0) {
                file.seek(colInfo.getNextColumnInfo());
                colInfo = new ColumnInfo(file);
                columns.add(colInfo);
                lastColumnInfo = colInfo;
            }
        }
    }
    
    @Override
    public synchronized void close() throws IOException {
        sync();
        file.close();
    }
    
    
    public synchronized Set<ColumnInfo> getColumns() {
        return new LinkedHashSet<ColumnInfo>(columns);
    }
    
    public synchronized void addColumns(String groupName, String [] columnNames) throws IOException {
        //write column information at the enf of the file.
        file.seek(file.length());
        ColumnInfo colInfo = new ColumnInfo(groupName, columnNames, file.getFilePointer());
        colInfo.writeTo(file);  
        //update refferences to this new ColumnInfo.
        lastColumnInfo.setNextColumnInfo(colInfo.getLocation(), file);
        toc.setLastColumnInfo(colInfo.getLocation(), file);
        lastColumnInfo = colInfo;
        columns.add(colInfo);
    }
    
    public void write(double[] values) {
        if (columns.size() < values.length) {
            
        }
    }
    
    public double[][] readAll(List<ColumnInfo> colInfos) {
        return null;
    }    
    
    public synchronized void sync() throws IOException {
        file.getFD().sync();
    }
}
