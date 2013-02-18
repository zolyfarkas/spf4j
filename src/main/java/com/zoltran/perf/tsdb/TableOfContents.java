/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.tsdb;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 *
 * @author zoly
 */
public class TableOfContents {
    private final long location;
    private long lastColumnInfo;
    private long lastDataFragment;
    private long firstColumnInfo;
    private long firstDataFragment;

    public TableOfContents(long location) {
        this.location = location; 
        firstColumnInfo = 0;
        firstDataFragment = 0;
        lastColumnInfo = 0;
        lastDataFragment = 0;    
    }
    
    public TableOfContents(RandomAccessFile raf) throws IOException {
        this.location = raf.getFilePointer();
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(this.location, 16, true);
        try {
            this.firstColumnInfo = raf.readLong();
            this.firstDataFragment = raf.readLong(); 
            this.lastColumnInfo = raf.readLong();
            this.lastDataFragment = raf.readLong();          
        } finally {
            lock.release();
        }
    }

    public void writeTo(RandomAccessFile raf) throws IOException {
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(this.location, 32, false);
        try {
            raf.seek(location);
            raf.writeLong(firstColumnInfo);
            raf.writeLong(firstDataFragment);            
            raf.writeLong(lastColumnInfo);
            raf.writeLong(lastDataFragment);
        } finally {
            lock.release();
        }
    }

    public long getLastColumnInfo() {
        return lastColumnInfo;
    }

    public long getLastDataFragment() {
        return lastDataFragment;
    }

    public long getFirstColumnInfo() {
        return firstColumnInfo;
    }

    public long getFirstDataFragment() {
        return firstDataFragment;
    }
    
    
    public void setLastColumnInfo(long lastColumnInfo,RandomAccessFile raf) throws IOException {
        this.lastColumnInfo = lastColumnInfo;
        FileChannel ch = raf.getChannel();
        long loc = location+16;
        FileLock lock = ch.lock(loc, 8, false);
        try {
            raf.seek(loc);           
            raf.writeLong(lastColumnInfo);
        } finally {
            lock.release();
        }
    }
    
    public void setLastDataFragment(long lastDataFragment,RandomAccessFile raf) throws IOException {
        this.lastDataFragment = lastDataFragment;
        FileChannel ch = raf.getChannel();
        long loc = location +24;        
        FileLock lock = ch.lock(loc, 8, false);
        try {
            raf.seek(loc);
            raf.writeLong(lastDataFragment);
        } finally {
            lock.release();
        }
    }

    
    public void setFirstColumnInfo(long firstColumnInfo, RandomAccessFile raf) throws IOException {
        this.firstColumnInfo = firstColumnInfo;
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(location, 8, false);
        try {
            raf.seek(location);
            raf.writeLong(firstColumnInfo);
        } finally {
            lock.release();
        }
    }

    public void setFirstDataFragment(long firstDataFragment, RandomAccessFile raf) throws IOException {
        this.firstDataFragment = firstDataFragment;
        FileChannel ch = raf.getChannel();
        long loc = location+8;
        FileLock lock = ch.lock(loc, 8, false);
        try {
            raf.seek(loc);
            raf.writeLong(firstDataFragment);            
        } finally {
            lock.release();
        }
    }

    
    
    
}
