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

import gnu.trove.list.array.TIntArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zoly
 */
public class DataFragment {
    private long location;        
    private long nextDataFragment;  
    private final long startTimeMillis;
    private List<double[]> data;
    private TIntArrayList timestamps;
    

    public DataFragment(long startTimeMillis) {      
        this.location = 0;
        this.nextDataFragment = 0;
        this.startTimeMillis = startTimeMillis;
        data = new ArrayList<double[]>();
        timestamps = new TIntArrayList();
    }

    public DataFragment(RandomAccessFile raf) throws IOException {
        location = raf.getFilePointer();
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(location, 8, true);
        try {
            this.nextDataFragment = raf.readLong();
            this.startTimeMillis = raf.readLong();
            int nrSamples = raf.readInt();
            int samplesLength = raf.readInt();
            int bufferSize = nrSamples * (samplesLength *8 + 4);
            byte [] buffer = new byte [bufferSize];
            raf.readFully(buffer);
            loadData(nrSamples, samplesLength, new DataInputStream(new ByteArrayInputStream(buffer)));            
        } finally {
            lock.release();
        }
    }

    public void writeTo(DataOutput dos) throws IOException {        
        dos.writeLong(nextDataFragment);
        dos.writeLong(startTimeMillis);
        dos.writeInt(data.size());
        dos.writeInt(data.get(0).length);
        for (int i=0; i< timestamps.size(); i++) {
            dos.writeInt(timestamps.get(i));
            for (double value: data.get(i)) {
                dos.writeDouble(value);
            }
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

    
    public void addData(long timestamp, double [] dataRow) {
        data.add(dataRow);
        timestamps.add( (int )(startTimeMillis - timestamp) );
    }
    
    
    public void setNextDataFragment(long nextDataFragment, RandomAccessFile raf) throws IOException {
        this.nextDataFragment = nextDataFragment;
        setNextDataFragment(location, nextDataFragment, raf);
    }
    
    
   public static void setNextDataFragment(long dataFragmentPosition, long nextDataFragment, RandomAccessFile raf) throws IOException {
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(raf.getFilePointer(),8,false);
        try {
            raf.seek(dataFragmentPosition);
            raf.writeLong(nextDataFragment);
        } finally {
            lock.release();
        }
    }

    public long getNextDataFragment() {
        return nextDataFragment;
    }

    public long getLocation() {
        return location;
    }

    private void loadData(int nrSamples, int samplesLength, DataInput raf) throws IOException {
        data = new ArrayList(nrSamples);
        timestamps = new TIntArrayList(nrSamples);
        for (int i=0; i< nrSamples;i++) {
            timestamps.add(raf.readInt());
            double [] row = new double[samplesLength];
            for(int j=0; j< samplesLength; j++) {
                row[j] = raf.readDouble();
            }                
            data.add(row);
        }
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public int getNrRows() {
        return data.size();
    }

    public List<double[]> getData() {
        return data;
    }

    public TIntArrayList getTimestamps() {
        return timestamps;
    }

    public void setLocation(long location) {
        this.location = location;
    }

    
    
    @Override
    public String toString() {
        return "DataFragment{" + "location=" + location + ", nextDataFragment=" + nextDataFragment + ", startTimeMillis=" + startTimeMillis + ", data=" + data + ", timestamps=" + timestamps + '}';
    }
    
    
    
    
}
