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
package com.zoltran.perf.tsdb;

import gnu.trove.map.hash.TObjectIntHashMap;
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
    private long firstDataFragment;
    private long lastDataFragment;
    private final String groupName;
    private final int sampleTime;
    private final byte[] groupMetaData;
    private final String [] columnNames;
    private final byte [][] columnMetaData;
    private final TObjectIntHashMap<String> nameToIndex;
    

    ColumnInfo(String groupName, byte [] groupMetaData, String [] columnNames, byte [][] columnMetaData, 
            int sampleTime, long location) {      
        this.location = location;
        this.nextColumnInfo = 0;
        this.firstDataFragment = 0;
        this.lastDataFragment = 0;
        this.groupName = groupName;
        this.sampleTime = sampleTime;
        this.groupMetaData = groupMetaData;
        this.columnNames = columnNames;
        this.columnMetaData = columnMetaData;
        this.nameToIndex = new TObjectIntHashMap<String>(columnNames.length + columnNames.length/3);
        for(int i=0; i< columnNames.length;i++) {
            this.nameToIndex.put(columnNames[i], i);
        }
    }

    ColumnInfo(RandomAccessFile raf) throws IOException {
        location = raf.getFilePointer();
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(location, 8, true);
        try {
            this.nextColumnInfo = raf.readLong();
            this.firstDataFragment = raf.readLong();
            this.lastDataFragment = raf.readLong();
            this.groupName = raf.readUTF();
            this.sampleTime = raf.readInt();
            int grMetaSize = raf.readInt();
            this.groupMetaData = new byte [grMetaSize];
            raf.readFully(groupMetaData);
            int nrColumns = raf.readShort();
            columnNames = new String[nrColumns];
            this.nameToIndex = new TObjectIntHashMap<String>(nrColumns+ nrColumns /3);
            for (int i=0; i< columnNames.length; i++) {
                String colName = raf.readUTF();
                columnNames[i] = colName;
                this.nameToIndex.put(colName, i);
            }
            columnMetaData = new byte[raf.readInt()][];
            for (int i=0; i< columnMetaData.length; i++) {
                int metaLength = raf.readInt();
                byte [] colMetaData = new byte[metaLength];
                raf.readFully(colMetaData);
                columnMetaData[i] = colMetaData;
            }
            
        } finally {
            lock.release();
        }
    }

    public void writeTo(DataOutput dos) throws IOException {        
        dos.writeLong(nextColumnInfo);
        dos.writeLong(firstDataFragment);
        dos.writeLong(lastDataFragment);
        dos.writeUTF(groupName);
        dos.writeInt(sampleTime);
        dos.writeInt(groupMetaData.length);
        dos.write(groupMetaData);
        dos.writeShort(columnNames.length);
        for (String columnName: columnNames) {
            dos.writeUTF(columnName);
        }
        dos.writeInt(columnMetaData.length);
        for (byte[] colMeta : columnMetaData) {        
            dos.writeInt(colMeta.length);
            dos.write(colMeta);              
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
        return columnNames.clone();
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

    public void setFirstDataFragment(long firstDataFragment, RandomAccessFile raf) throws IOException {
        this.firstDataFragment = firstDataFragment;
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(raf.getFilePointer()+8,8,false);
        try {
            raf.seek(location+8);
            raf.writeLong(firstDataFragment);
        } finally {
            lock.release();
        }
    }
    
    
    public void setLastDataFragment(long lastDataFragment, RandomAccessFile raf) throws IOException {
        this.lastDataFragment = lastDataFragment;
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(raf.getFilePointer()+16,8,false);
        try {
            raf.seek(location+16);
            raf.writeLong(lastDataFragment);
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

    public String getGroupName() {
        return groupName;
    }

    public long getFirstDataFragment() {
        return firstDataFragment;
    }

    public long getLastDataFragment() {
        return lastDataFragment;
    }

    public byte[][] getColumnMetaData() {
        return columnMetaData.clone();
    }
    
    public int getColumnIndex(String columnName) {
        Integer result = this.nameToIndex.get(columnName);
        if (result == null) {
            return -1;
        } else {
            return result;
        }
    }

    public int getSampleTime() {
        return sampleTime;
    }

    public byte[] getGroupMetaData() {
        return groupMetaData.clone();
    }
    
    
 
    
}
