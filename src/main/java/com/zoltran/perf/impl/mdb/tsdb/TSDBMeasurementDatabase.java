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
package com.zoltran.perf.impl.mdb.tsdb;

import com.google.common.base.Charsets;
import com.zoltran.base.AbstractRunnable;
import com.zoltran.base.Arrays;
import com.zoltran.base.DefaultScheduler;
import com.zoltran.base.Pair;
import com.zoltran.perf.EntityMeasurementsInfo;
import com.zoltran.perf.MeasurementDatabase;
import com.zoltran.perf.impl.chart.Charts;
import com.zoltran.perf.tsdb.ColumnInfo;
import com.zoltran.perf.tsdb.TimeSeriesDatabase;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.ThreadSafe;
import javax.imageio.ImageIO;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@ThreadSafe
public class TSDBMeasurementDatabase implements MeasurementDatabase, Closeable, TSDBMeasurementDatabaseMBean {



    private final TimeSeriesDatabase database;
    
    private volatile ScheduledFuture<?> future;
    
    private static final Logger LOG = LoggerFactory.getLogger(TSDBMeasurementDatabase.class);

    public TSDBMeasurementDatabase(final String databaseName) throws IOException {
        this.database = new TimeSeriesDatabase(databaseName, new byte[] {});
    }
    private static final AtomicInteger dbCount = new AtomicInteger(0);

    public void registerJmx() throws MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this,
                new ObjectName("SPF4J:name=TSDBMeasurementDatabase" + dbCount.getAndIncrement()));
    }
    
    public void closeOnShutdown () {
        Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable(false) {

            @Override
            public void doRun() throws Exception {
                close();
            }
        }, "tsdb shutdown"));
    }
    
    public void flushEvery(int intervalMillis) {
        future = DefaultScheduler.INSTANCE.scheduleAtFixedRate(new AbstractRunnable(false) {

            @Override
            public void doRun() throws Exception {
                database.flush();
            }
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void alocateMeasurements(EntityMeasurementsInfo measurement, int sampleTimeMillis) throws IOException {
        String groupName = measurement.getMeasuredEntity().toString();
        if (!database.hasColumnGroup(groupName)) {
            String [] measurementNames = measurement.getMeasurementNames();
            byte [] uom = measurement.getUnitOfMeasurement().getBytes(Charsets.UTF_8);
            byte [][] metaData = new byte [measurementNames.length] [];
            for(int i=0;i< metaData.length; i++) { 
                metaData[i] =uom;
            }
            database.addColumnGroup(groupName, sampleTimeMillis, measurementNames, 
                    metaData);
        }
    }
    
    
    
    @Override
    public void saveMeasurements(EntityMeasurementsInfo measurementInfo, long [] measurements, long timeStampMillis, int sampleTimeMillis) throws IOException {
        String groupName = measurementInfo.getMeasuredEntity().toString();
        if (!database.hasColumnGroup(groupName)) {
            alocateMeasurements(measurementInfo, sampleTimeMillis);
        }
        database.write(timeStampMillis, groupName, measurements);   
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        if (future != null) {
            future.cancel(false);
        }
       database.close();
    }

    private static String fixName(String name) {
        StringBuilder result = new StringBuilder(name.length());
        for (int i=0;i<name.length();i++) {
            char c = name.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    
    
    @Override
    public List<String> generateCharts(int width, int height) throws IOException {
        long startTime = new LocalDate().withDayOfWeek(DateTimeConstants.MONDAY).toDateTimeAtStartOfDay().getMillis();
        long endTime = System.currentTimeMillis();
        return generateCharts(startTime, endTime, width, height);
    }
    
    
    @Override
    public List<String> generateCharts(long startTimeMillis, long endTimeMillis,
            int width, int height) throws IOException {
        database.flush();
        List<String> result = new ArrayList<String>();
        Collection<ColumnInfo> columnsInfo = database.getColumnsInfo();
        for (ColumnInfo info : columnsInfo) {
            String [] columns = info.getColumnNames();
            Pair<long[], long[][]> data = database.read(info.getGroupName(), startTimeMillis, endTimeMillis);
            if (data.getFirst().length >0) {
                result.add(generateMinMaxAvgChart(info, data, width, height));
                result.add(generateHeatChart(info, data, width, height));
            }
        }
        LOG.info("Generated charts {}", result);
        return result;
    }


    private String generateMinMaxAvgChart( 
            ColumnInfo info, Pair<long[], long[][]> data,
            int width, int height) throws IOException {
        long [][] vals = data.getSecond();
        double [] min = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("min"));
        double [] max = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("max"));
        double [] total = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("total"));
        double [] count = Arrays.getColumnAsDoubles(vals, info.getColumnIndex("count"));
        long [] timestamps = data.getFirst();
        BufferedImage combined = Charts.createMinMaxAvgCountImg("Measurements for "
                + info.getGroupName() + " generated by spf4j",
                timestamps, min, max, total, count, width, height);
        File dbFile = new File(database.getDBFilePath());
        File graphicFile = File.createTempFile(dbFile.getName() + "_" + fixName(info.getGroupName()), ".mmac.png",
                dbFile.getParentFile());
        ImageIO.write(combined, "png", graphicFile);
        return graphicFile.getPath();
    }
    
    
    
    
    
    
    public static Pair<long[], double [][]> fillGaps(long[] timestamps, long [][] data , int sampleTime, int nrColumns) {
       long startTime = timestamps[0];
       int nrSamples = (int) ((timestamps[timestamps.length-1] - startTime )  / sampleTime);
       long [] lts = new long [nrSamples];  
       double [][] dr = new double [nrSamples][];
       long nextTime = startTime;
       int j=0;
       int maxDeviation = sampleTime/2;
       double [] nodata = new double[nrColumns];
       for(int i=0; i<nrColumns; i++) {
           nodata[i] = Double.NaN;
       }
       for(int i=0; i< nrSamples; i++) {
          lts[i] = nextTime;
          
          if (Math.abs(timestamps[j] - nextTime) < maxDeviation) {
              dr[i] = Arrays.toDoubleArray(data[j]);
              j++;
          } else {
              dr[i] = nodata;           
          }        
          nextTime+=sampleTime;
       }
       return new Pair<long[], double[][]>(lts, dr);
    }
     
    private String generateHeatChart(ColumnInfo info, Pair<long[], long[][]> data,
            int width, int height) throws IOException {
        File dbFile = new File(database.getDBFilePath());
        File graphicFile = File.createTempFile(dbFile.getName() + "_" + fixName(info.getGroupName()), ".dist.png",
                dbFile.getParentFile());
        Pair<long[], double [][]> mData = fillGaps(data.getFirst(), data.getSecond(), 
                info.getSampleTime(), info.getColumnNames().length);
        Charts.createHeatChart("Measurements distribution for "
                + info.getGroupName() + " generated by spf4j", graphicFile, info.getColumnNames(), mData.getSecond(),
                data.getFirst()[0], info.getSampleTime(), width, height);
        return graphicFile.getAbsolutePath();
    }
    
    
    @Override
    public List<String> generate(Properties props) throws IOException {
        int width = Integer.valueOf(props.getProperty("width", "1200"));
        int height = Integer.valueOf(props.getProperty("height", "800"));
        long startTime = Long.valueOf(props.getProperty("startTime",
                Long.toString(new LocalDate().withDayOfWeek(DateTimeConstants.MONDAY).toDate().getTime())));
        long endTime = Long.valueOf(props.getProperty("endTime", Long.toString(System.currentTimeMillis())));
        return generateCharts(startTime, endTime, width, height);
    }

    @Override
    public List<String> getParameters() {
        return java.util.Arrays.asList("width", "height", "startTime", "endTime");
    }

}
