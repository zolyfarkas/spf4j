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
import com.zoltran.perf.EntityMeasurementsInfo;
import com.zoltran.perf.MeasurementDatabase;
import com.zoltran.perf.impl.chart.Charts;
import com.zoltran.perf.tsdb.TimeSeriesDatabase;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.*;
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
        database.addColumns(measurement.getMeasuredEntity().toString(), measurementNames, 
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
          throw new UnsupportedOperationException();
//        List<String> result = new ArrayList<String>();
//
//        for (Map.Entry<Pair<EntityMeasurementsInfo, Integer>, RrdDb> entry : databases.asMap().entrySet()) {
//            syncDb(entry.getValue());
//            result.add(generateMinMaxAvgChart(startTimeMillis, endTimeMillis, entry.getValue(), width, height));
//            result.add(generateHeatChart(startTimeMillis, endTimeMillis, entry.getValue(), width, height));
//        }
//
//        LOG.info("Generated charts {}", result);
//        return result;
    }


    private static String generateMinMaxAvgChart(long startTimeMillis, long endTimeMillis, final RrdDb rrdDb,
            int width, int height) throws IOException {
           throw new UnsupportedOperationException();
//        FetchRequest request = rrdDb.createFetchRequest(ConsolFun.FIRST, startTimeMillis / 1000,
//                endTimeMillis / 1000);
//        request.setFilter("min", "max", "total", "count");
//        final FetchData data = request.fetchData();
//        double [] min = data.getValues("min");
//        double [] max = data.getValues("max");
//        double [] total = data.getValues("total");
//        double [] count = data.getValues("count");
//        long [] timestamps = data.getTimestamps();
//        BufferedImage combined = Charts.createMinMaxAvgCountImg(timestamps, min, max, total, count, rrdDb, width, height);
//        File graphicFile = File.createTempFile(new File(rrdDb.getPath()).getName(), ".png",
//                new File(rrdDb.getCanonicalPath()).getParentFile());
//        ImageIO.write(combined, "png", graphicFile);
//        return graphicFile.getPath();
    }
    



    
    
    private static String generateHeatChart(long startTimeMillis, long endTimeMillis, final RrdDb rrdDb,
            int width, int height) throws IOException {
        FetchRequest request = rrdDb.createFetchRequest(ConsolFun.FIRST, startTimeMillis / 1000,
                endTimeMillis / 1000);
        final FetchData data = request.fetchData();
        String [] dsNames = data.getDsNames();
        double [][] values = data.getValues();
        File rrdFile = new File(rrdDb.getPath());
        File graphicFile = File.createTempFile(rrdFile.getName(), ".png",
                new File(rrdDb.getCanonicalPath()).getParentFile());
        Charts.createHeatChart(rrdFile.getName(), graphicFile, dsNames, values, data.getFirstTimestamp()*1000, data.getStep()*1000, width, height);
        return graphicFile.getAbsolutePath();
    }
    
    
    private static final Method syncMethod;

    static {
        try {
            syncMethod = RrdNioBackend.class.getDeclaredMethod("sync");
            syncMethod.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void syncDb(RrdDb entry) throws RuntimeException {
        Object backend = entry.getRrdBackend();
        if (backend instanceof RrdNioBackend) {
            try {
                syncMethod.invoke(backend);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
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
