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
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Yet another time series database. Why? because all the other ts databases had
 * various constraints that restrict the functionality I can add to spf4j.
 *
 * Initial Features:
 *
 * 1. measurements can be added dynamically anytime to a database. 2. long
 * measurement names. 3. the stored interval is not known from the beginning. 4.
 * implementation biased towards write performance.
 *
 * Future thoughts:
 *
 *
 *
 * @author zoly
 */
public class TimeSeriesDatabase implements Closeable {

    public static final int VERSION = 1;
    private final Map<String, ColumnInfo> groups;
    private final RandomAccessFile file;
    private final Header header;
    private final TableOfContents toc;
    private ColumnInfo lastColumnInfo;
    private final Map<String, DataFragment> writeDataFragments;
    private final String pathToDatabaseFile;

    public TimeSeriesDatabase(String pathToDatabaseFile, byte[] metaData) throws IOException {
        this.pathToDatabaseFile = pathToDatabaseFile;
        file = new RandomAccessFile(pathToDatabaseFile, "rw");
        // read or create header
        if (file.length() == 0) {
            this.header = new Header(VERSION, metaData);
            this.header.writeTo(file);
            this.toc = new TableOfContents(file.getFilePointer());
            this.toc.writeTo(file);
        } else {
            this.header = new Header(file);
            this.toc = new TableOfContents(file);
        }
        groups = new HashMap<String, ColumnInfo>();
        if (toc.getFirstColumnInfo() > 0) {
            file.seek(toc.getFirstColumnInfo());
            ColumnInfo colInfo = new ColumnInfo(file);
            groups.put(colInfo.getGroupName(), colInfo);

            lastColumnInfo = colInfo;
            while (colInfo.getNextColumnInfo() > 0) {
                file.seek(colInfo.getNextColumnInfo());
                colInfo = new ColumnInfo(file);
                groups.put(colInfo.getGroupName(), colInfo);
                lastColumnInfo = colInfo;
            }
        }
        writeDataFragments = new HashMap<String, DataFragment>();
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            flush();
        } finally {
            file.close();
        }
    }

    public synchronized boolean hasColumnGroup(String groupName) {
        return groups.containsKey(groupName);
    }
    
    
    public synchronized void addColumnGroup(String groupName, byte [] groupMetaData, int sampleTime, String[] columnNames, byte[][] metaData) throws IOException {
        if (groups.containsKey(groupName)) {
            throw new IllegalArgumentException("group already exists " + groupName);
        }
        //write column information at the enf of the file.
        flush();
        file.seek(file.length());
        ColumnInfo colInfo = new ColumnInfo(groupName, groupMetaData, columnNames, metaData,sampleTime, file.getFilePointer());
        colInfo.writeTo(file);
        //update refferences to this new ColumnInfo.
        if (lastColumnInfo != null) {
            lastColumnInfo.setNextColumnInfo(colInfo.getLocation(), file);
        } else {
            toc.setFirstColumnInfo(colInfo.getLocation(), file);
        }
        toc.setLastColumnInfo(colInfo.getLocation(), file);
        lastColumnInfo = colInfo;
        groups.put(groupName, colInfo);
    }

    public synchronized void write(long time, String groupName, long[] values) throws IOException {
        if (!groups.containsKey(groupName)) {
            throw new IllegalArgumentException("Unknown group name" + groupName);
        }
        DataFragment writeDataFragment = writeDataFragments.get(groupName);
        if (writeDataFragment == null) {
            writeDataFragment = new DataFragment(time);
            writeDataFragments.put(groupName, writeDataFragment);
        }
        writeDataFragment.addData(time, values);
    }

    public synchronized void flush() throws IOException {
        for (Map.Entry<String, DataFragment> entry : writeDataFragments.entrySet()) {
            DataFragment writeDataFragment = entry.getValue();
            String groupName = entry.getKey();
            file.seek(file.length());
            writeDataFragment.setLocation(file.getFilePointer());
            writeDataFragment.writeTo(file);
            ColumnInfo colInfo = groups.get(groupName);
            if (colInfo.getLastDataFragment() != 0) {
                DataFragment.setNextDataFragment(colInfo.getLastDataFragment(), writeDataFragment.getLocation(), file);
            } else {
                colInfo.setFirstDataFragment(writeDataFragment.getLocation(), file);
            }
            colInfo.setLastDataFragment(writeDataFragment.getLocation(), file);
        }
        writeDataFragments.clear();
        sync();
    }

    public synchronized String[] getColumnNames(String groupName) {
        return groups.get(groupName).getColumnNames();
    }
    
    
    public synchronized Collection<ColumnInfo> getColumnsInfo() {
        return groups.values();
    }

    public synchronized Pair<long[], long[][]> readAll(String groupName) throws IOException {
        return read(groupName, 0, Long.MAX_VALUE);
    }
    
    public synchronized Pair<long[], long[][]> read(String groupName, long startTime, long endTime) throws IOException {
        TLongArrayList timeStamps = new TLongArrayList();
        List<long[]> data = new ArrayList<long[]>();
        ColumnInfo info = groups.get(groupName);

        if (info.getFirstDataFragment() > 0) {
            DataFragment frag;
            long nextFragmentLocation = info.getFirstDataFragment();
            do {
                file.seek(nextFragmentLocation);
                frag = new DataFragment(file);
                long fragStartTime = frag.getStartTimeMillis();
                if (fragStartTime > startTime) {
                    TIntArrayList fragTimestamps = frag.getTimestamps();
                    int nr = 0;
                    for (int i = 0; i < fragTimestamps.size(); i++) {
                        long ts = fragStartTime + fragTimestamps.get(i);
                        if (ts < endTime) {
                            timeStamps.add(ts);
                            nr++;
                        } else {
                            break;
                        }
                    }
                    List<long[]> d = frag.getData();
                    for (int i=0; i< nr; i++) {
                        data.add(d.get(i));
                    }
                    if (fragTimestamps.size() > nr) {
                        break;
                    }
                }
                nextFragmentLocation = frag.getNextDataFragment();
            }
            while (nextFragmentLocation > 0);
        }
        return new Pair<long[], long[][]>(timeStamps.toArray(), data.toArray(new long[data.size()][]));
    }

    public synchronized void sync() throws IOException {
        file.getFD().sync();
    }
    
    public String getDBFilePath() {
        return pathToDatabaseFile;
    }

    @Override
    public String toString() {
        return "TimeSeriesDatabase{" + "groups=" + groups + ", pathToDatabaseFile=" + pathToDatabaseFile + '}';
    }
    
    
    
}
