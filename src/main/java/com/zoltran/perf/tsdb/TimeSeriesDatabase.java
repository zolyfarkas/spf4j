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

import com.zoltran.base.Pair;
import gnu.trove.list.array.TLongArrayList;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Yet another time series database. Why? because all the other ts databases had
 * various constraints that restrict the functionality I can add to spf4j.
 *
 * Initial Features: 
 * 
 * 1. measurements can be added dynamically anytime to a database. 
 * 2. long measurement names.
 * 3. the stored interval is not known from the beginning.
 * 4. implementation biased towards write performance.
 * 
 * Future thoughts:
 * 
 * 
 *
 * @author zoly
 */

public class TimeSeriesDatabase implements Closeable {
    
    public static final int VERSION = 1;
    private final List<ColumnInfo> columns;
    Set<String> groupNames;
    
    private final RandomAccessFile file;
    private final Header header;
    private final TableOfContents toc;
    private ColumnInfo lastColumnInfo;
    private DataFragment lastDataFragment;
    
    private DataFragment writeDataFragment;
  
   
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
            this.toc = new TableOfContents(file);
        }
        columns = new ArrayList<ColumnInfo>();
        groupNames = new HashSet<String>();
        if (toc.getFirstColumnInfo() > 0) {
            file.seek(toc.getFirstColumnInfo());
            ColumnInfo colInfo = new ColumnInfo(file);
            columns.add(colInfo);
            groupNames.add(colInfo.getGroupName());
            lastColumnInfo = colInfo;
            while (colInfo.getNextColumnInfo() > 0) {
                file.seek(colInfo.getNextColumnInfo());
                colInfo = new ColumnInfo(file);
                columns.add(colInfo);
                groupNames.add(colInfo.getGroupName());
                lastColumnInfo = colInfo;
            }
        }
        if (toc.getLastDataFragment() >0) {
            file.seek(toc.getLastDataFragment());
            lastDataFragment = new DataFragment(file);
        }
    }
    
    @Override
    public synchronized void close() throws IOException {
        flush();
        file.close();
    }
    
    
    public synchronized void addColumns(String groupName, String [] columnNames) throws IOException {
        if (groupNames.contains(groupName)) {
            throw new IllegalArgumentException("group already exists " + groupName);
        }
        //write column information at the enf of the file.
        flush();
        file.seek(file.length());
        ColumnInfo colInfo = new ColumnInfo(groupName, columnNames, file.getFilePointer());
        colInfo.writeTo(file);  
        //update refferences to this new ColumnInfo.
        if (lastColumnInfo != null ) {
            lastColumnInfo.setNextColumnInfo(colInfo.getLocation(), file);
        } else {
            toc.setFirstColumnInfo(colInfo.getLocation(), file);
        }
        toc.setLastColumnInfo(colInfo.getLocation(), file);
        lastColumnInfo = colInfo;
        columns.add(colInfo);
        groupNames.add(groupName);
    }
    
    public synchronized void write(long time, double[] values) throws IOException {       
        if (writeDataFragment == null) {
            file.seek(file.length());
            writeDataFragment = new DataFragment(time, file.getFilePointer());
        }    
        writeDataFragment.addData(time, values);
    }
    
    
    public synchronized void flush() throws IOException {
        if (writeDataFragment != null) {
            writeDataFragment.writeTo(file);
            if (lastDataFragment != null) {
                lastDataFragment.setNextDataFragment(writeDataFragment.getLocation(), file);
            } else {
                toc.setFirstDataFragment(writeDataFragment.getLocation(), file);
            }
            lastDataFragment = writeDataFragment;
            toc.setLastDataFragment(writeDataFragment.getLocation(), file);
            writeDataFragment = null;
            sync();
        }
    }
    
    public synchronized Set<ColumnInfo> getColumns() {
        return new LinkedHashSet<ColumnInfo>(columns);
    }
    
    public synchronized Pair<TLongArrayList,List<double[]>> readAll() throws IOException {
        TLongArrayList timeStamps = new TLongArrayList();
        List<double[]> data = new ArrayList<double[]>();
        if (toc.getFirstDataFragment() >0) {
            file.seek( toc.getFirstDataFragment() );       
            DataFragment frag = new DataFragment(file);
            timeStamps.addAll(frag.getTimestamps());
            data.addAll(frag.getData());
            while (frag.getNextDataFragment() > 0) {
                file.seek( frag.getNextDataFragment() );
                frag = new DataFragment(file);
                timeStamps.addAll(frag.getTimestamps());
                data.addAll(frag.getData());
            }                   
        }       
        return new Pair<TLongArrayList, List<double[]>>(timeStamps, data);
    }    
    
    
    
    public synchronized void sync() throws IOException {
        file.getFD().sync();
    }
}
