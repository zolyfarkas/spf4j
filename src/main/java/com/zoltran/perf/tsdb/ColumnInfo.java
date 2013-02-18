/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.tsdb;

import com.google.common.base.Charsets;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 *
 * @author zoly
 */
public class ColumnInfo {
    private final long location;        
    private long nextColumnInfo;  
    private final String groupName;
    private final String [] columnNames;
    
    

    public ColumnInfo(String groupName, String [] columnNames, long location) {      
        this.location = location;
        this.nextColumnInfo = 0;
        this.groupName = groupName;
        this.columnNames = columnNames;
    }

    public ColumnInfo(RandomAccessFile raf) throws IOException {
        location = raf.getFilePointer();
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(location, 8, true);
        try {
            this.nextColumnInfo = raf.readLong();
            this.groupName = raf.readUTF();
            int nrColumns = raf.readShort();
            columnNames = new String[nrColumns];
            for (int i=0; i< columnNames.length; i++) {
                columnNames[i] = raf.readUTF();
            }
        } finally {
            lock.release();
        }
    }

    public void writeTo(DataOutput dos) throws IOException {        
        dos.writeLong(nextColumnInfo);
        dos.writeUTF(groupName);
        dos.writeShort(columnNames.length);
        for (String columnName: columnNames) {
            dos.writeUTF(columnName);
        }
    }
    
    public void writeTo(RandomAccessFile raf) throws IOException {        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutput dos = new DataOutputStream(bos);
        writeTo(dos);
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(raf.getFilePointer(),8,false);
        try {
            raf.seek(location);
            raf.write(bos.toByteArray());
        } finally {
            lock.release();
        }
    }

    public String [] getColumnNames() {
        return columnNames;
    }

    public void setNextColumnInfo(long nextColumnInfo, RandomAccessFile raf) throws IOException {
        this.nextColumnInfo = nextColumnInfo;
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(raf.getFilePointer(),8,false);
        try {
            raf.seek(location);
            raf.writeLong(nextColumnInfo);
        } finally {
            lock.release();
        }
    }

    public long getNextColumnInfo() {
        return nextColumnInfo;
    }

    public long getLocation() {
        return location;
    }
    
    
    
}
