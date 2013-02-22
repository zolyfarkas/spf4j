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
import java.io.FileNotFoundException;
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

    public TimeSeriesDatabase(String pathToDatabaseFile, byte[] metaData) throws FileNotFoundException, IOException {
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

    public synchronized void addColumns(String groupName, String[] columnNames, byte[][] metaData) throws IOException {
        if (groups.containsKey(groupName)) {
            throw new IllegalArgumentException("group already exists " + groupName);
        }
        //write column information at the enf of the file.
        flush();
        file.seek(file.length());
        ColumnInfo colInfo = new ColumnInfo(groupName, columnNames, metaData, file.getFilePointer());
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

    public synchronized void write(long time, String groupName, double[] values) throws IOException {
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

    public synchronized Pair<TLongArrayList, List<double[]>> readAll(String groupName) throws IOException {
        TLongArrayList timeStamps = new TLongArrayList();
        List<double[]> data = new ArrayList<double[]>();
        ColumnInfo info = groups.get(groupName);

        if (info.getFirstDataFragment() > 0) {
            file.seek(info.getFirstDataFragment());
            DataFragment frag = new DataFragment(file);
            long fragStartTime = frag.getStartTimeMillis();
            TIntArrayList fragTimestamps = frag.getTimestamps();
            for (int i = 0; i < fragTimestamps.size(); i++) {
                timeStamps.add(fragStartTime + fragTimestamps.get(i));
            }
            data.addAll(frag.getData());
            while (frag.getNextDataFragment() > 0) {
                file.seek(frag.getNextDataFragment());
                frag = new DataFragment(file);
                fragStartTime = frag.getStartTimeMillis();
                fragTimestamps = frag.getTimestamps();
                for (int i = 0; i < fragTimestamps.size(); i++) {
                    timeStamps.add(fragStartTime + fragTimestamps.get(i));
                }
                data.addAll(frag.getData());
            }
        }
        return new Pair<TLongArrayList, List<double[]>>(timeStamps, data);
    }

    public synchronized void sync() throws IOException {
        file.getFD().sync();
    }
}
