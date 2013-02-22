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
import java.util.*;
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

    @Override
    public void alocateMeasurements(EntityMeasurementsInfo measurement, int sampleTimeMillis) throws IOException {
        String [] measurementNames = measurement.getMeasurementNames();
        byte [] uom = measurement.getUnitOfMeasurement().getBytes(Charsets.UTF_8);
        byte [][] metaData = new byte [measurementNames.length] [];
        for(int i=0;i< metaData.length; i++) { 
            metaData[i] =uom;
        }
        database.addColumns(measurement.getMeasuredEntity().toString(), sampleTimeMillis, measurementNames, 
                metaData);
    }
    
    
    
    @Override
    public void saveMeasurements(EntityMeasurementsInfo measurementInfo, long [] measurements, long timeStampMillis, int sampleTimeMillis) throws IOException {
        database.write(timeStampMillis, measurementInfo.getMeasuredEntity().toString(), measurements);   
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
       database.close();
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
        List<String> result = new ArrayList<String>();
        Collection<ColumnInfo> columnsInfo = database.getColumnsInfo();
        for (ColumnInfo info : columnsInfo) {
            String [] columns = info.getColumnNames();
            Pair<long[], long[][]> data = database.read(info.getGroupName(), startTimeMillis, endTimeMillis);
            result.add(generateMinMaxAvgChart(info, data, width, height));
            result.add(generateHeatChart(info, data, width, height));
        }
        LOG.info("Generated charts {}", result);
        return result;
    }


    private String generateMinMaxAvgChart( 
            ColumnInfo info, Pair<long[], long[][]> data,
            int width, int height) throws IOException {
        long [][] vals = data.getSecond();
        double [] min = toDoubleArray(vals[info.getColumnIndex("min")]);
        double [] max = toDoubleArray(vals[info.getColumnIndex("max")]);
        double [] total = toDoubleArray(vals[info.getColumnIndex("total")]);
        double [] count = toDoubleArray(vals[info.getColumnIndex("count")]);
        long [] timestamps = data.getFirst();
        BufferedImage combined = Charts.createMinMaxAvgCountImg("Measurements for "
                + info.getGroupName() + " generated by spf4j",
                timestamps, min, max, total, count, width, height);
        File graphicFile = File.createTempFile(new File(database.getDBFilePath()).getName(), ".png",
                new File(database.getDBFilePath()).getParentFile());
        ImageIO.write(combined, "png", graphicFile);
        return graphicFile.getPath();
    }
    
    public static double[] toDoubleArray(long [] larr) {
        double [] result = new double[larr.length];
        for (int i=0; i< larr.length;i++) {
            result[i] = larr[i];
        }
        return result;
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
              dr[i] = toDoubleArray(data[j]);
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
        File graphicFile = File.createTempFile(database.getDBFilePath(), ".png",
                new File(database.getDBFilePath()).getParentFile());
        Pair<long[], double [][]> mData = fillGaps(data.getFirst(), data.getSecond(), 
                info.getSampleTime(), info.getColumnNames().length);
        Charts.createHeatChart("Measurements distribution for "
                + info.getGroupName() + " generated by spf4j", graphicFile, info.getColumnNames(), mData.getSecond(),
                data.getFirst()[0], info.getSampleTime(), width, height);
        return graphicFile.getAbsolutePath();
    }
    
    
    
    @Override
    public List<String> getMeasurements() throws IOException {
           throw new UnsupportedOperationException();
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
        return Arrays.asList("width", "height", "startTime", "endTime");
    }

}
