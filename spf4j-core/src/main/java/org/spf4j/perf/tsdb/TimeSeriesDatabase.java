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
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
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
 * 1. measurements can be added dynamically anytime to a database.
 * 2. long measurement names.
 * 3. the stored interval is not known from the beginning.
 * 4. implementation biased towards write performance.
 *
 * Format:
 * 
 * Header: TSDB[version:int][metadata:bytes]
 * Table of Contents: firstTableInfoPtr, lastTableInfoPtr
 * TableInfo:
 * DataFragment:
 * ......
 * ......
 * TableInfo:
 * DataFragment:
 * EOF
 * 
 * @author zoly
 */
public final class TimeSeriesDatabase implements Closeable {
    
    public static final int VERSION = 1;
    private final ConcurrentMap<String, TSTable> tables;
    private final RandomAccessFile file;
    private final Header header;
    private TableOfContents toc;
    private TSTable lastTableInfo;
    // per table buffer of data to be written
    private final Map<String, DataFragment> writeDataFragments;
    private final String path;
    private final FileChannel ch;
    
    public TimeSeriesDatabase(final String pathToDatabaseFile) throws IOException {
        this(pathToDatabaseFile, false);
    }
    
    public TimeSeriesDatabase(final String pathToDatabaseFile, final byte ... metaData) throws IOException {
        this(pathToDatabaseFile, true, metaData);
    }
    
    public TimeSeriesDatabase(final String pathToDatabaseFile, final boolean isWrite, final byte... metaData)
            throws IOException {
        file = new RandomAccessFile(pathToDatabaseFile, isWrite ? "rw" : "r");
        // uniques per process string for sync purposes.
        this.path = new File(pathToDatabaseFile).getPath().intern();
        tables = new ConcurrentHashMap<>();
        writeDataFragments = new HashMap<>();
        // read or create header
        synchronized (path) {
            this.ch = file.getChannel();
            FileLock lock;
            if (isWrite) {
                lock = ch.lock();
            } else {
                lock = ch.lock(0, Long.MAX_VALUE, true);
            }
            try {
                if (file.length() == 0) {
                    this.header = new Header(VERSION, metaData);
                    this.header.writeTo(file);
                    this.toc = new TableOfContents(file.getFilePointer());
                    this.toc.writeTo(file);
                } else {
                    this.header = new Header(file);
                    this.toc = new TableOfContents(file);
                }
            } catch (IOException | RuntimeException e) {
                try {
                    lock.release();
                    throw e;
                } catch (IOException ex) {
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
            lock.release();
            lock = ch.lock(0, Long.MAX_VALUE, true);
            try {
                readTableInfos();
            } catch (IOException | RuntimeException e) {
                try {
                    lock.release();
                    throw e;
                } catch (IOException ex) {
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
            lock.release();
        }
    }

    public void reReadTableInfos() throws IOException {
        synchronized (path) {
            FileLock lock = ch.lock(0, Long.MAX_VALUE, true);
            try {
                toc = new TableOfContents(file, toc.getLocation()); // reread toc
                readTableInfos();
            } catch (IOException | RuntimeException e) {
                try {
                    lock.release();
                    throw e;
                } catch (IOException ex) {
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
            lock.release();
        }
    }
    
    private void readTableInfos() throws IOException {
        final long firstColumnInfo = toc.getFirstTableInfoPtr();
        if (firstColumnInfo > 0) {
            file.seek(firstColumnInfo);
            TSTable colInfo = new TSTable(file);
            tables.put(colInfo.getTableName(), colInfo);
            lastTableInfo = colInfo;
            while (colInfo.getNextTSTable() > 0) {
                file.seek(colInfo.getNextTSTable());
                colInfo = new TSTable(file);
                tables.put(colInfo.getTableName(), colInfo);
                lastTableInfo = colInfo;
            }
        }
    }
    
    private void readLastTableInfo() throws IOException {
        toc = new TableOfContents(file, toc.getLocation()); // reread toc
        if (toc.getLastTableInfoPtr() == 0) {
            return;
        }
        lastTableInfo = new TSTable(file, toc.getLastTableInfoPtr()); // update last table info
    }
    
    @Override
    public void close() throws IOException {
        synchronized (path) {
            try (RandomAccessFile vfile = this.file) {
                flush();
            }
        }
    }
    
    public boolean hasTSTable(final String tableName) {
        synchronized (path) {
            return tables.containsKey(tableName);
        }
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
    
    public void addTSTable(final String tableName,
            final byte[] tableMetaData, final int sampleTime, final String[] columnNames,
            final byte[][] columnMetaData) throws IOException {
        synchronized (path) {
            if (hasTSTable(tableName)) {
                throw new IllegalArgumentException("group already exists " + tableName);
            }
            flush();
            FileLock lock = ch.lock();
            TSTable colInfo;
            try {
                readLastTableInfo();
                //write column information at the end of the file.
                file.seek(file.length());
                colInfo = new TSTable(tableName, tableMetaData, columnNames,
                        columnMetaData, sampleTime, file.getFilePointer());
                colInfo.writeTo(file);
                //update refferences to this new TableInfo.
                if (lastTableInfo != null) {
                    lastTableInfo.setNextColumnInfo(colInfo.getLocation(), file);
                } else {
                    toc.setFirstTableInfo(colInfo.getLocation(), file);
                }
                toc.setLastTableInfo(colInfo.getLocation(), file);
            } catch (IOException | RuntimeException e) {
                try {
                    lock.release();
                    throw e;
                } catch (IOException ex) {
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
            lock.release();
            
            lastTableInfo = colInfo;
            tables.put(tableName, colInfo);
        }
    }
    
    public void write(final long time, final String tableName, final long[] values) throws IOException {
        if (!hasTSTable(tableName)) {
            throw new IllegalArgumentException("Unknown table name" + tableName);
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
    
    public void flush() throws IOException {
        synchronized (path) {
            List<Map.Entry<String, DataFragment>> lwriteDataFragments;
            synchronized (writeDataFragments) {
                if (writeDataFragments.isEmpty()) {
                    return;
                }
                lwriteDataFragments = new ArrayList<>(writeDataFragments.entrySet());
                writeDataFragments.clear();
            }
            FileLock lock = ch.lock();
            try {
                for (Map.Entry<String, DataFragment> entry : lwriteDataFragments) {
                    DataFragment writeDataFragment = entry.getValue();
                    String groupName = entry.getKey();
                    file.seek(file.length());
                    writeDataFragment.setLocation(file.getFilePointer());
                    writeDataFragment.writeTo(file);
                    TSTable colInfo = tables.get(groupName);
                    colInfo = new TSTable(file, colInfo.getLocation()); // reread colInfo
                    tables.put(groupName, colInfo); // update colInfo
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
            } catch (IOException | RuntimeException e) {
                try {
                    lock.release();
                    throw e;
                } catch (IOException ex) {
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
            lock.release();
        }
    }
    
    public String[] getColumnNames(final String tableName) {
        synchronized (path) {
            return tables.get(tableName).getColumnNames();
        }
    }
    
    public TSTable getTSTable(final String tableName) {
        synchronized (path) {
            return new TSTable(tables.get(tableName));
        }
    }
    
    public  Collection<TSTable> getTSTables() {
        synchronized (path) {
            Collection<TSTable> result = new ArrayList<>(tables.size());
            for (TSTable table : tables.values()) {
                result.add(new TSTable(table));
            }
            return result;
        }
    }
    
    public Map<String, TSTable> getTsTables() {
        synchronized (path) {
           Map<String, TSTable> result = new HashMap<>(tables.size());
           for (Map.Entry<String, TSTable> entry : tables.entrySet()) {
               result.put(entry.getKey(), new TSTable(entry.getValue()));
           }
           return result;
        }
    }
    
    public TimeSeries readAll(final String tableName) throws IOException {
        synchronized (path) {
            return read(tableName, 0, Long.MAX_VALUE);
        }
    }

    public long readStartDate(final String tableName) throws IOException {
        synchronized (path) {
            TSTable info = tables.get(tableName);
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
    }

    public TimeSeries read(final String tableName,
            final long startTime, final long endTime) throws IOException {
        synchronized (path) {
            TSTable info = tables.get(tableName);
            final long firstDataFragment = info.getFirstDataFragment();
            return read(startTime, endTime, firstDataFragment, info.getLastDataFragment(), false);
        }
    }

    /**
     * Read measurements from table.
     *
     * @param tableName
     * @param startTime start time including
     * @param endTime end time including
     * @return
     * @throws IOException
     */
    private TimeSeries read(
            final long startTime, final long endTime,
            final long startAtFragment, final long endAtFragment,
            final boolean skipFirst)
            throws IOException {
        synchronized (path) {
            TLongArrayList timeStamps = new TLongArrayList();
            List<long[]> data = new ArrayList<>();
            if (startAtFragment > 0) {
                FileLock lock = ch.lock(0, Long.MAX_VALUE, true);
                try {
                    DataFragment frag;
                    long nextFragmentLocation = startAtFragment;
                    boolean last = false;
                    boolean psFirst = skipFirst;
                    do {
                        if (nextFragmentLocation == endAtFragment) {
                            last = true;
                        }
                        file.seek(nextFragmentLocation);
                        frag = new DataFragment(file);
                        if (psFirst) {
                            psFirst = false;
                        } else {
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
                            int i = 0;
                            for (long[] d : frag.getData()) {
                                if (i < nr) {
                                    data.add(d);
                                } else {
                                    break;
                                }
                                nr++;
                            }
                            if (fragTimestamps.size() > nr) {
                                break;
                            }
                        }
                        }
                        nextFragmentLocation = frag.getNextDataFragment();
                    } while (nextFragmentLocation > 0 && !last);
                } catch (IOException | RuntimeException e) {
                    try {
                        lock.release();
                        throw e;
                    } catch (IOException ex) {
                        ex.addSuppressed(e);
                        throw ex;
                    }
                }
                lock.release();
            }
            return new TimeSeries(timeStamps.toArray(), data.toArray(new long[data.size()][]));
        }
    }
    
    private void sync() throws IOException {
        file.getFD().sync();
    }
    
    public String getDBFilePath() {
        return path;
    }
    
    public JFreeChart createHeatJFreeChart(final String tableName) throws IOException {
        return createHeatJFreeChart(tableName, 0, Long.MAX_VALUE);
    }
    
    public JFreeChart createHeatJFreeChart(final String tableName, final long startTime,
            final long endTime) throws IOException {
        TSTable info = this.getTSTable(tableName);
        TimeSeries data = this.read(tableName, startTime, endTime);
        return createHeatJFreeChart(data, info);
    }
    
    public JFreeChart createMinMaxAvgJFreeChart(final String tableName) throws IOException {
        return createMinMaxAvgJFreeChart(tableName, 0, Long.MAX_VALUE);
    }
    
    public JFreeChart createMinMaxAvgJFreeChart(final String tableName, final long startTime,
            final long endTime) throws IOException {
        TSTable info = this.getTSTable(tableName);
        TimeSeries data = this.read(tableName, startTime, endTime);
        return createMinMaxAvgJFreeChart(data, info);
    }

    public JFreeChart createCountJFreeChart(final String tableName) throws IOException {
        return createCountJFreeChart(tableName, 0, Long.MAX_VALUE);
    }
    
    public JFreeChart createCountJFreeChart(final String tableName, final long startTime,
            final long endTime) throws IOException {
        TSTable info = this.getTSTable(tableName);
        TimeSeries data = this.read(tableName, startTime, endTime);
        return createCountJFreeChart(data, info);
    }
    
    public List<JFreeChart> createJFreeCharts(final String tableName) throws IOException {
        return createJFreeCharts(tableName, 0, Long.MAX_VALUE);
    }
    
    public List<JFreeChart> createJFreeCharts(final String tableName, final long startTime,
            final long endTime) throws IOException {
        TSTable info = this.getTSTable(tableName);
        TimeSeries data = this.read(tableName, startTime, endTime);
        return createJFreeCharts(data, info);
    }
    
    public static JFreeChart createHeatJFreeChart(final TimeSeries data, final TSTable info) {
        Pair<long[], double[][]> mData = fillGaps(data.getTimeStamps(), data.getValues(),
                info.getSampleTime(), info.getColumnNames().length);
        final int totalColumnIndex = info.getColumnIndex("total");
        return Charts.createHeatJFreeChart(info.getColumnNames(),
                mData.getSecond(), data.getTimeStamps()[0], info.getSampleTime(),
                new String(info.getColumnMetaData()[totalColumnIndex], Charsets.UTF_8), "Measurements distribution for "
                + info.getTableName() + ", sampleTime " + info.getSampleTime() + "ms, generated by spf4j");
    }
    
    public static JFreeChart createMinMaxAvgJFreeChart(final TimeSeries data, final TSTable info) {
        long[][] vals = data.getValues();
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
        long[] timestamps = data.getTimeStamps();
        return Charts.createTimeSeriesJFreeChart("Min,Max,Avg chart for "
                + info.getTableName() + ", sampleTime " + info.getSampleTime() + "ms, generated by spf4j", timestamps,
                new String[]{"min", "max", "avg"}, new String(info.getColumnMetaData()[totalColumnIndex],
                        Charsets.UTF_8), new double[][]{min, max, Arrays.divide(total, count)});
    }
    
    public static JFreeChart createCountJFreeChart(final TimeSeries data, final TSTable info) {
        long[][] vals = data.getValues();
        double[] count = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("count"));
        long[] timestamps = data.getTimeStamps();
        return Charts.createTimeSeriesJFreeChart("count chart for "
                + info.getTableName() + ", sampleTime " + info.getSampleTime() + " ms, generated by spf4j", timestamps,
                new String[]{"count"}, "count", new double[][]{count});
    }
    
    public static List<JFreeChart> createJFreeCharts(final TimeSeries data, final TSTable info) {
        long[][] vals = data.getValues();
        List<JFreeChart> result = new ArrayList<>();
        Map<String, Pair<List<String>, List<double[]>>> measurementsByUom = new HashMap<>();
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
        long[] timestamps = data.getTimeStamps();
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
        TimeSeries data = readAll(tableName);
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), Charsets.UTF_8))) {
            Csv.writeCsvElement("timestamp", writer);
            for (String colName : table.getColumnNames()) {
                writer.append(',');
                Csv.writeCsvElement(colName, writer);
            }
            writer.write('\n');
            long[] timestamps = data.getTimeStamps();
            long[][] values = data.getValues();
            for (int i = 0; i < timestamps.length; i++)  {
                Csv.writeCsvElement(formatter.print(timestamps[i]), writer);
                for (long val : values[i]) {
                    writer.append(',');
                    Csv.writeCsvElement(Long.toString(val), writer);
                }
                writer.write('\n');
            }
        }
    }
    
    public void writeCsvTables(final List<String> tableNames, final File output) throws IOException {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), Charsets.UTF_8))) {
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
                TimeSeries data = readAll(tableName);
                long[] timestamps = data.getTimeStamps();
                long[][] values = data.getValues();
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
        }
    }
    
    @Override
    public String toString() {
        return "TimeSeriesDatabase{" + "groups=" + tables + ", pathToDatabaseFile=" + path + '}';
    }
    
    @SuppressFBWarnings("MDM_THREAD_YIELD")
    public void tail(final long pollMillis, final long from, final TSDataHandler handler)
            throws IOException {
        Map<String, TSTable> lastState = new HashMap<>();
        long lastSize = 0;
        while (!Thread.interrupted() && !handler.finish()) {
            long currSize = this.file.length();
            if (currSize > lastSize) {
                // see if we have new Tables;
                reReadTableInfos();
                Map<String, TSTable> currState = getTsTables();
                for (String tableName : Sets.difference(currState.keySet(), lastState.keySet())) {
                    handler.newTable(tableName, currState.get(tableName).getColumnNames());
                }
                for (TSTable table : currState.values()) {
                    final String tableName = table.getTableName();
                    TSTable prevTableState = lastState.get(tableName);
                    final long currLastDataFragment = table.getLastDataFragment();

                    if (prevTableState == null) {
                        long lastDataFragment = table.getFirstDataFragment();
                        if (lastDataFragment > 0) {
                            TimeSeries data = read(from, Long.MAX_VALUE, lastDataFragment, currLastDataFragment,
                                    false);
                            handler.newData(tableName, data);
                        }
                    } else {
                        long lastDataFragment = prevTableState.getLastDataFragment();
                        if (lastDataFragment == 0) {
                            lastDataFragment = table.getFirstDataFragment();
                            if (lastDataFragment > 0) {
                                TimeSeries data = read(from, Long.MAX_VALUE, lastDataFragment, currLastDataFragment,
                                        false);
                                handler.newData(tableName, data);
                            }
                        } else if (currLastDataFragment > lastDataFragment) {
                            TimeSeries data = read(from, Long.MAX_VALUE, lastDataFragment, currLastDataFragment,
                                    true);
                            handler.newData(tableName, data);
                        }
                    }
                }
                lastState = currState;
            }
            lastSize = currSize;
            try {
                Thread.sleep(pollMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    
    
}
