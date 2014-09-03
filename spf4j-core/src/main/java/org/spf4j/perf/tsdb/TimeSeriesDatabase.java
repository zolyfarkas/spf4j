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
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jfree.chart.JFreeChart;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.spf4j.base.Arrays;
import org.spf4j.io.Csv;
import org.spf4j.perf.impl.chart.Charts;
import static org.spf4j.perf.impl.chart.Charts.fillGaps;

/**
 * Yet another time series database. Why? because all the other ts databases had various constraints that restrict the
 * functionality I can add to spf4j.
 *
 * Initial Features:
 *
 * 1. measurements can be added dynamically anytime to a database. 2. long measurement names. 3. the stored interval is
 * not known from the beginning. 4. implementation biased towards write performance.
 *
 * Future thoughts:
 *
 *
 *
 * @author zoly
 */
public final class TimeSeriesDatabase implements Closeable {
    
    public static final int VERSION = 1;
    private final ConcurrentMap<String, TSTable> groups;
    private final RandomAccessFile file;
    private final Header header;
    private final TableOfContents toc;
    private TSTable lastColumnInfo;
    private final Map<String, DataFragment> writeDataFragments;
    private final String pathToDatabaseFile;
    
    public TimeSeriesDatabase(final String pathToDatabaseFile, final byte... metaData) throws IOException {
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
        groups = new ConcurrentHashMap<String, TSTable>();
        final long firstColumnInfo = toc.getFirstColumnInfo();
        if (firstColumnInfo > 0) {
            file.seek(firstColumnInfo);
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
    
    public void addTSTable(final String tableName,
            final byte[] tableMetaData, final int sampleTime, final String[] columnNames,
            final String[] columnMetaData) throws IOException {
        byte[][] metadata = new byte[columnMetaData.length][];
        for (int i = 0; i < columnMetaData.length; i++) {
            metadata[i] = columnMetaData[i].getBytes(Charsets.UTF_8);
        }
        addTSTable(tableName, tableMetaData, sampleTime, columnNames, metadata);
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
    
    public void write(final long time, final String tableName, final long[] values) throws IOException {
        if (!groups.containsKey(tableName)) {
            throw new IllegalArgumentException("Unknown group name" + tableName);
        }
        synchronized (writeDataFragments) {
            DataFragment writeDataFragment = writeDataFragments.get(tableName);
            if (writeDataFragment == null) {
                writeDataFragment = new DataFragment(time);
                writeDataFragments.put(tableName, writeDataFragment);
            }
            writeDataFragment.addData(time, values);
        }
    }
    
    public synchronized void flush() throws IOException {
        List<Map.Entry<String, DataFragment>> lwriteDataFragments;
        synchronized (writeDataFragments) {
            lwriteDataFragments = new ArrayList<Map.Entry<String, DataFragment>>(writeDataFragments.size());
            for (Map.Entry<String, DataFragment> entry : writeDataFragments.entrySet()) {
                lwriteDataFragments.add(entry);
            }
            writeDataFragments.clear();
        }

        for (Map.Entry<String, DataFragment> entry : lwriteDataFragments) {
            DataFragment writeDataFragment = entry.getValue();
            String groupName = entry.getKey();
            file.seek(file.length());
            writeDataFragment.setLocation(file.getFilePointer());
            writeDataFragment.writeTo(file);
            TSTable colInfo = groups.get(groupName);
            final long lastDataFragment = colInfo.getLastDataFragment();
            final long location = writeDataFragment.getLocation();
            if (lastDataFragment != 0) {
                DataFragment.setNextDataFragment(lastDataFragment, location, file);
            } else {
                colInfo.setFirstDataFragment(location, file);
            }
            colInfo.setLastDataFragment(location, file);
        }
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

    
    public synchronized long readStartDate(final String tableName) throws IOException {
        TSTable info = groups.get(tableName);
        long nextFragmentLocation = info.getFirstDataFragment();
        if (nextFragmentLocation > 0) {
            DataFragment frag;
            file.seek(nextFragmentLocation);
            frag = new DataFragment(file);
            return frag.getStartTimeMillis();
        } else {
            return -1;
        }
    }
    
    /**
     * Read measurements from table.
     * @param tableName
     * @param startTime start time including
     * @param endTime end time including
     * @return
     * @throws IOException
     */
    
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("LII_LIST_INDEXED_ITERATING")
    public synchronized Pair<long[], long[][]> read(final String tableName,
            final long startTime, final long endTime) throws IOException {
        TLongArrayList timeStamps = new TLongArrayList();
        List<long[]> data = new ArrayList<long[]>();
        TSTable info = groups.get(tableName);
        final long firstDataFragment = info.getFirstDataFragment();
        if (firstDataFragment > 0) {
            DataFragment frag;
            long nextFragmentLocation = firstDataFragment;
            do {
                file.seek(nextFragmentLocation);
                frag = new DataFragment(file);
                long fragStartTime = frag.getStartTimeMillis();
                if (fragStartTime >= startTime) {
                    TIntArrayList fragTimestamps = frag.getTimestamps();
                    int nr = 0;
                    for (int i = 0; i < fragTimestamps.size(); i++) {
                        long ts = fragStartTime + fragTimestamps.get(i);
                        if (ts <= endTime) {
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
    
    private void sync() throws IOException {
        file.getFD().sync();
    }
    
    public String getDBFilePath() {
        return pathToDatabaseFile;
    }
    
    public JFreeChart createHeatJFreeChart(final String tableName) throws IOException {
        return createHeatJFreeChart(tableName, 0, Long.MAX_VALUE);
    }
    
    public JFreeChart createHeatJFreeChart(final String tableName, final long startTime,
            final long endTime) throws IOException {
        TSTable info = this.getTSTable(tableName);
        Pair<long[], long[][]> data = this.read(tableName, startTime, endTime);
        return createHeatJFreeChart(data, info);
    }
    
    public JFreeChart createMinMaxAvgJFreeChart(final String tableName) throws IOException {
        return createMinMaxAvgJFreeChart(tableName, 0, Long.MAX_VALUE);
    }
    
    public JFreeChart createMinMaxAvgJFreeChart(final String tableName, final long startTime,
            final long endTime) throws IOException {
        TSTable info = this.getTSTable(tableName);
        Pair<long[], long[][]> data = this.read(tableName, startTime, endTime);
        return createMinMaxAvgJFreeChart(data, info);
    }

    public JFreeChart createCountJFreeChart(final String tableName) throws IOException {
        return createCountJFreeChart(tableName, 0, Long.MAX_VALUE);
    }
    
    public JFreeChart createCountJFreeChart(final String tableName, final long startTime,
            final long endTime) throws IOException {
        TSTable info = this.getTSTable(tableName);
        Pair<long[], long[][]> data = this.read(tableName, startTime, endTime);
        return createCountJFreeChart(data, info);
    }
    
    public List<JFreeChart> createJFreeCharts(final String tableName) throws IOException {
        return createJFreeCharts(tableName, 0, Long.MAX_VALUE);
    }
    
    public List<JFreeChart> createJFreeCharts(final String tableName, final long startTime,
            final long endTime) throws IOException {
        TSTable info = this.getTSTable(tableName);
        Pair<long[], long[][]> data = this.read(tableName, startTime, endTime);
        return createJFreeCharts(data, info);
    }
    
    public static JFreeChart createHeatJFreeChart(final Pair<long[], long[][]> data, final TSTable info) {
        Pair<long[], double[][]> mData = fillGaps(data.getFirst(), data.getSecond(),
                info.getSampleTime(), info.getColumnNames().length);
        final int totalColumnIndex = info.getColumnIndex("total");
        return Charts.createHeatJFreeChart(info.getColumnNames(),
                mData.getSecond(), data.getFirst()[0], info.getSampleTime(),
                new String(info.getColumnMetaData()[totalColumnIndex], Charsets.UTF_8), "Measurements distribution for "
                + info.getTableName() + ", sampleTime " + info.getSampleTime() + "ms, generated by spf4j");
    }
    
    public static JFreeChart createMinMaxAvgJFreeChart(final Pair<long[], long[][]> data, final TSTable info) {
        long[][] vals = data.getSecond();
        double[] min = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("min"));
        double[] max = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("max"));
        final int totalColumnIndex = info.getColumnIndex("total");
        double[] total = Arrays.getColumnAsDoubles(vals, totalColumnIndex);
        double[] count = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("count"));
        for (int i = 0; i < count.length; i++) {
            if (count[i] == 0) {
                min[i] = 0;
                max[i] = 0;
            }
        }
        long[] timestamps = data.getFirst();
        return Charts.createTimeSeriesJFreeChart("Min,Max,Avg chart for "
                + info.getTableName() + ", sampleTime " + info.getSampleTime() + "ms, generated by spf4j", timestamps,
                new String[]{"min", "max", "avg"}, new String(info.getColumnMetaData()[totalColumnIndex],
                        Charsets.UTF_8), new double[][]{min, max, Arrays.divide(total, count)});
    }
    
    public static JFreeChart createCountJFreeChart(final Pair<long[], long[][]> data, final TSTable info) {
        long[][] vals = data.getSecond();
        double[] count = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("count"));
        long[] timestamps = data.getFirst();
        return Charts.createTimeSeriesJFreeChart("count chart for "
                + info.getTableName() + ", sampleTime " + info.getSampleTime() + " ms, generated by spf4j", timestamps,
                new String[]{"count"}, "count", new double[][]{count});
    }
    
    public static List<JFreeChart> createJFreeCharts(final Pair<long[], long[][]> data, final TSTable info) {
        long[][] vals = data.getSecond();
        List<JFreeChart> result = new ArrayList<JFreeChart>();
        Map<String, Pair<List<String>, List<double[]>>> measurementsByUom =
                new HashMap<String, Pair<List<String>, List<double[]>>>();
        String[] columnMetaData = info.getColumnMetaDataAsStrings();
        for (int i = 0; i < info.getColumnNumber(); i++) {
            String uom = columnMetaData[i];
            Pair<List<String>, List<double[]>> meas = measurementsByUom.get(uom);
            if (meas == null) {
                meas = Pair.of((List<String>) new ArrayList<String>(), (List<double[]>) new ArrayList<double[]>());
                measurementsByUom.put(uom, meas);
            }
            meas.getFirst().add(info.getColumnName(i));
            meas.getSecond().add(Arrays.getColumnAsDoubles(vals, i));
        }
        long[] timestamps = data.getFirst();
        for (Map.Entry<String, Pair<List<String>, List<double[]>>> entry : measurementsByUom.entrySet()) {
            Pair<List<String>, List<double[]>> p = entry.getValue();
            final List<String> measurementNames = p.getFirst();
            final List<double[]> measurements = p.getSecond();
            result.add(Charts.createTimeSeriesJFreeChart("chart for "
                    + info.getTableName() + ", sampleTime " + info.getSampleTime()
                    + " ms, generated by spf4j", timestamps,
                    measurementNames.toArray(new String[measurementNames.size()]), entry.getKey(),
                    measurements.toArray(new double[measurements.size()][])));
        }
        return result;
    }
    
    public byte[] getMetaData() {
        return header.getMetaData().clone();
    }
    
    public void writeCsvTable(final String tableName, final File output) throws IOException {
        TSTable table = getTSTable(tableName);
        Pair<long[], long[][]> data = readAll(tableName);
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), Charsets.UTF_8));
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        try {
            Csv.writeCsvElement("timestamp", writer);
            for (String colName : table.getColumnNames()) {
                writer.append(',');
                Csv.writeCsvElement(colName, writer);
            }
            writer.write('\n');
            long[] timestamps = data.getFirst();
            long[][] values = data.getSecond();
            for (int i = 0; i < timestamps.length; i++)  {
                Csv.writeCsvElement(formatter.print(timestamps[i]), writer);
                for (long val : values[i]) {
                    writer.append(',');
                    Csv.writeCsvElement(Long.toString(val), writer);
                }
                writer.write('\n');
            }
        } finally {
            writer.close();
        }
    }

    
    public void writeCsvTables(final List<String> tableNames, final File output) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), Charsets.UTF_8));
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        try {
            String firstTable = tableNames.get(0);
            TSTable table = getTSTable(firstTable);
            Csv.writeCsvElement("table", writer);
            writer.append(',');
            Csv.writeCsvElement("timestamp", writer);
            for (String colName : table.getColumnNames()) {
                writer.append(',');
                Csv.writeCsvElement(colName, writer);
            }
            writer.write('\n');
            
            for (String tableName : tableNames) {
                Pair<long[], long[][]> data = readAll(tableName);
                long[] timestamps = data.getFirst();
                long[][] values = data.getSecond();
                for (int i = 0; i < timestamps.length; i++)  {
                    Csv.writeCsvElement(tableName, writer);
                    writer.append(',');
                    Csv.writeCsvElement(formatter.print(timestamps[i]), writer);
                    for (long val : values[i]) {
                        writer.append(',');
                        Csv.writeCsvElement(Long.toString(val), writer);
                    }
                    writer.write('\n');
                }
            }
        } finally {
            writer.close();
        }
    }
    
    
    
    @Override
    public String toString() {
        return "TimeSeriesDatabase{" + "groups=" + groups + ", pathToDatabaseFile=" + pathToDatabaseFile + '}';
    }
}
