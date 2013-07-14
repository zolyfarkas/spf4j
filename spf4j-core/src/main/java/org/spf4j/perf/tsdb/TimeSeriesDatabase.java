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
package org.spf4j.perf.tsdb;

import com.google.common.base.Charsets;
import org.spf4j.base.Pair;
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
import org.jfree.chart.JFreeChart;
import org.spf4j.base.Arrays;
import org.spf4j.perf.impl.chart.Charts;
import static org.spf4j.perf.impl.chart.Charts.fillGaps;

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
public final class TimeSeriesDatabase implements Closeable {

    public static final int VERSION = 1;
    private final Map<String, TSTable> groups;
    private final RandomAccessFile file;
    private final Header header;
    private final TableOfContents toc;
    private TSTable lastColumnInfo;
    private final Map<String, DataFragment> writeDataFragments;
    private final String pathToDatabaseFile;

    public TimeSeriesDatabase(final String pathToDatabaseFile, final byte[] metaData) throws IOException {
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
        groups = new HashMap<String, TSTable>();
        if (toc.getFirstColumnInfo() > 0) {
            file.seek(toc.getFirstColumnInfo());
            TSTable colInfo = new TSTable(file);
            groups.put(colInfo.getTableName(), colInfo);

            lastColumnInfo = colInfo;
            while (colInfo.getNextTSTable() > 0) {
                file.seek(colInfo.getNextTSTable());
                colInfo = new TSTable(file);
                groups.put(colInfo.getTableName(), colInfo);
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

    public synchronized boolean hasTSTable(final String tableName) {
        return groups.containsKey(tableName);
    }

    public synchronized void addTSTable(final String tableName,
            final byte[] tableMetaData, final int sampleTime, final String[] columnNames,
            final byte[][] columnMetaData) throws IOException {
        if (groups.containsKey(tableName)) {
            throw new IllegalArgumentException("group already exists " + tableName);
        }
        //write column information at the end of the file.
        flush();
        file.seek(file.length());
        TSTable colInfo = new TSTable(tableName, tableMetaData, columnNames,
                columnMetaData, sampleTime, file.getFilePointer());
        colInfo.writeTo(file);
        //update refferences to this new ColumnInfo.
        if (lastColumnInfo != null) {
            lastColumnInfo.setNextColumnInfo(colInfo.getLocation(), file);
        } else {
            toc.setFirstColumnInfo(colInfo.getLocation(), file);
        }
        toc.setLastColumnInfo(colInfo.getLocation(), file);
        lastColumnInfo = colInfo;
        groups.put(tableName, colInfo);
    }

    public synchronized void write(final long time, final String tableName, final long[] values) throws IOException {
        if (!groups.containsKey(tableName)) {
            throw new IllegalArgumentException("Unknown group name" + tableName);
        }
        DataFragment writeDataFragment = writeDataFragments.get(tableName);
        if (writeDataFragment == null) {
            writeDataFragment = new DataFragment(time);
            writeDataFragments.put(tableName, writeDataFragment);
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
            TSTable colInfo = groups.get(groupName);
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

    public synchronized String[] getColumnNames(final String tableName) {
        return groups.get(tableName).getColumnNames();
    }

    public synchronized TSTable getTSTable(final String tableName) {
        return groups.get(tableName);
    }

    public synchronized Collection<TSTable> getTSTables() {
        return groups.values();
    }

    public synchronized Pair<long[], long[][]> readAll(final String tableName) throws IOException {
        return read(tableName, 0, Long.MAX_VALUE);
    }

    public synchronized Pair<long[], long[][]> read(final String tableName,
            final long startTime, final long endTime) throws IOException {
        TLongArrayList timeStamps = new TLongArrayList();
        List<long[]> data = new ArrayList<long[]>();
        TSTable info = groups.get(tableName);

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
                    for (int i = 0; i < nr; i++) {
                        data.add(d.get(i));
                    }
                    if (fragTimestamps.size() > nr) {
                        break;
                    }
                }
                nextFragmentLocation = frag.getNextDataFragment();
            } while (nextFragmentLocation > 0);
        }
        return Pair.of(timeStamps.toArray(), data.toArray(new long[data.size()][]));
    }

    private synchronized void sync() throws IOException {
        file.getFD().sync();
    }

    public String getDBFilePath() {
        return pathToDatabaseFile;
    }

    public JFreeChart createHeatJFreeChart(final String tableName) throws IOException {
        TSTable info = this.getTSTable(tableName);
        Pair<long[], long[][]> data = this.readAll(tableName);
        return createHeatJFreeChart(data, info);
    }

    public JFreeChart createMinMaxAvgJFreeChart(final String tableName) throws IOException {
        TSTable info = this.getTSTable(tableName);
        Pair<long[], long[][]> data = this.readAll(tableName);
        return createMinMaxAvgJFreeChart(data, info);
    }

    public JFreeChart createCountJFreeChart(final String tableName) throws IOException {
        TSTable info = this.getTSTable(tableName);
        Pair<long[], long[][]> data = this.readAll(tableName);
        return createCountJFreeChart(data, info);
    }
    

    public static JFreeChart createHeatJFreeChart(final Pair<long[], long[][]> data, final TSTable info) {
        Pair<long[], double[][]> mData = fillGaps(data.getFirst(), data.getSecond(),
                info.getSampleTime(), info.getColumnNames().length);
        JFreeChart chart = Charts.createHeatJFreeChart(info.getColumnNames(),
                mData.getSecond(), data.getFirst()[0], info.getSampleTime(),
                new String(info.getTableMetaData(), Charsets.UTF_8), "Measurements distribution for "
                + info.getTableName() + " generated by spf4j");
        return chart;
    }

    public static JFreeChart createMinMaxAvgJFreeChart(final Pair<long[], long[][]> data, final TSTable info) {
        long[][] vals = data.getSecond();
        double[] min = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("min"));
        double[] max = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("max"));
        double[] total = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("total"));
        double[] count = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("count"));
        for (int i = 0; i < count.length; i++) {
            if (count[i] == 0) {
                min[i] = 0;
                max[i] = 0;
            }
        }
        long[] timestamps = data.getFirst();
        return Charts.createTimeSeriesJFreeChart("Min,Max,Avg chart for "
                + info.getTableName() + " generated by spf4j", timestamps,
                new String[]{"min", "max", "avg"}, new String(info.getTableMetaData(), Charsets.UTF_8),
                new double[][]{min, max, Arrays.divide(total, count)});
    }
    
    
    public static JFreeChart createCountJFreeChart(final Pair<long[], long[][]> data, final TSTable info) {
        long[][] vals = data.getSecond();
        double[] count = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("count"));
        long[] timestamps = data.getFirst();
        return Charts.createTimeSeriesJFreeChart("count chart for "
                + info.getTableName() + " generated by spf4j", timestamps,
                new String[]{"count"}, "count", new double[][]{count});
    }
    
    
    @Override
    public String toString() {
        return "TimeSeriesDatabase{" + "groups=" + groups + ", pathToDatabaseFile=" + pathToDatabaseFile + '}';
    }
    
}
