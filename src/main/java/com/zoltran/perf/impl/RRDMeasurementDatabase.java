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
package com.zoltran.perf.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zoltran.base.Pair;
import com.zoltran.perf.EntityMeasurements;
import com.zoltran.perf.EntityMeasurementsInfo;
import com.zoltran.perf.MeasurementDatabase;
import java.awt.Color;
import java.awt.image.BufferedImage;
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
import javax.imageio.ImageIO;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYSeriesLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@ThreadSafe
public class RRDMeasurementDatabase implements MeasurementDatabase, Closeable, RRDMeasurementDatabaseMBean {

    private final String databaseFolder;
    private final LoadingCache<Pair<EntityMeasurementsInfo, Integer>, RrdDb> databases;
    private static final Logger LOG = LoggerFactory.getLogger(RRDMeasurementDatabase.class);

    public RRDMeasurementDatabase(final String databaseFolder) {
        this.databaseFolder = databaseFolder;
        databases = CacheBuilder.newBuilder().maximumSize(2048).build(
                new CacheLoader<Pair<EntityMeasurementsInfo, Integer>, RrdDb>() {
                    @Override
                    public RrdDb load(Pair<EntityMeasurementsInfo, Integer> key) throws Exception {
                        int sampleTimeSeconds = key.getSecond();
                        EntityMeasurementsInfo emeasurements = key.getFirst();
                        String rrdFilePath = databaseFolder + File.separator + getDBName(emeasurements,
                                sampleTimeSeconds);
                        File rrdFile = new File(rrdFilePath);
                        if (rrdFile.exists()) {
                            return new RrdDb(rrdFilePath);
                        } else {
                            RrdDef rrdDef = new RrdDef(rrdFilePath, sampleTimeSeconds);
                            rrdDef.addArchive(ConsolFun.FIRST, 0.5, 1, SECONDS_PER_WEEK / sampleTimeSeconds); // 1 week worth of data at original granularity.
                            rrdDef.setStartTime(System.currentTimeMillis() / 1000);
                            int heartbeat = sampleTimeSeconds * 2;
                            for (String mName : emeasurements.getMeasurementNames()) {
                                rrdDef.addDatasource(mName, DsType.GAUGE, heartbeat, Double.NaN, Double.NaN);
                            }
                            return new RrdDb(rrdDef);
                        }

                    }
                });

    }
    private static final AtomicInteger dbCount = new AtomicInteger(0);

    public void registerJmx() throws MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this,
                new ObjectName("SPF4J:name=RRDMeasurementDatabase" + dbCount.getAndIncrement()));
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
    
    private static String getDBName(EntityMeasurementsInfo measurement, int sampleTimeSeconds) {
        return fixName(measurement.getMeasuredEntity().toString()) + "_" + sampleTimeSeconds + "_"
                + measurement.getUnitOfMeasurement() + "_" + new LocalDate().getWeekOfWeekyear() + ".rrd4j";
    }
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_WEEK = SECONDS_PER_HOUR * 24 * 7;

    private static long msToS(long ms) {
        long result = ms / 1000;
        if ((ms % 1000) > 500) {
            result++;
        }
        return result;
    }

    private static int msToS(int ms) {
        int result = ms / 1000;
        if ((ms % 1000) > 500) {
            result++;
        }
        return result;
    }

    @Override
    public void saveMeasurements(EntityMeasurements measurement, long timeStampMillis, int sampleTimeMillis) throws IOException {
        Map<String, Number> measurements = measurement.getMeasurements(true);
        
        RrdDb rrdDb = databases.getUnchecked(new Pair(measurement.getInfo(), msToS(sampleTimeMillis)));
        Sample sample = rrdDb.createSample(msToS(timeStampMillis));
        for (Map.Entry<String, Number> entry : measurements.entrySet()) {
            sample.setValue(entry.getKey(), entry.getValue().doubleValue());
        }
        try {
            sample.update();
            LOG.debug("Measurement {} persisted at {}", measurement.getInfo(), timeStampMillis);
        } catch (IOException e) {
            throw new IOException("Cannot persist sample " + measurement.getInfo() + " at " + timeStampMillis, e);
        } catch (RuntimeException e) {
            throw new IOException("Cannot persist sample " + measurement.getInfo() + " at " + timeStampMillis, e);
        }
    }

    @PreDestroy
    @Override
    public void close() throws IOException {
        for (RrdDb db : databases.asMap().values()) {
            db.close();
        }
        databases.invalidateAll();
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

        for (Map.Entry<Pair<EntityMeasurementsInfo, Integer>, RrdDb> entry : databases.asMap().entrySet()) {
            syncDb(entry.getValue());
            result.add(generateMinMaxAvgChart(startTimeMillis, endTimeMillis, entry.getValue(), width, height));
            result.add(generateHeatChart(startTimeMillis, endTimeMillis, entry.getValue(), width, height));
        }

        LOG.info("Generated charts {}", result);
        return result;
    }


    private static String generateMinMaxAvgChart(long startTimeMillis, long endTimeMillis, final RrdDb rrdDb,
            int width, int height) throws IOException {
        FetchRequest request = rrdDb.createFetchRequest(ConsolFun.FIRST, startTimeMillis / 1000,
                endTimeMillis / 1000);
        request.setFilter("min", "max", "total", "count");
        final FetchData data = request.fetchData();
        double [] min = data.getValues("min");
        double [] max = data.getValues("max");
        double [] total = data.getValues("total");
        double [] count = data.getValues("count");
        long [] timestamps = data.getTimestamps();
        BufferedImage bi = createMinMaxAvgImg(timestamps, min, max, total, count, rrdDb, width, height-height/3);
        
        BufferedImage bi2 = createCountImg(timestamps, count, rrdDb, width, height/3);
        
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
        combined.getGraphics().drawImage(bi, 0, 0, null);
        combined.getGraphics().drawImage(bi2, 0, height-height/3, null);

        File graphicFile = File.createTempFile(new File(rrdDb.getPath()).getName(), ".png",
                new File(rrdDb.getCanonicalPath()).getParentFile());
        ImageIO.write(combined, "png", graphicFile);
        return graphicFile.getPath();
    }
    
    private static BufferedImage createMinMaxAvgImg(long[] timestamps, double[] min, double[] max, double[] total, double[] count, final RrdDb rrdDb, int width, int height)
    {
        TimeSeries minTs = new TimeSeries("min");
        TimeSeries maxTs = new TimeSeries("max");
        TimeSeries avgTs = new TimeSeries("avg");
        for (int i=0;i<timestamps.length; i++) {
            FixedMillisecond ts = new FixedMillisecond(timestamps[i] *1000);
            minTs.add(ts, min[i]);
            maxTs.add(ts, max[i]);
            avgTs.add(ts, total[i]/count[i]);
        }
        TimeSeriesCollection timeseriescollection = new TimeSeriesCollection();
        timeseriescollection.addSeries(minTs);
        timeseriescollection.addSeries(maxTs);
        timeseriescollection.addSeries(avgTs);
        JFreeChart jfreechart = ChartFactory.createTimeSeriesChart("Measurements for " + rrdDb.getPath(), 
                "Time", "Value", timeseriescollection, true, true, false);
        XYPlot xyplot = (XYPlot)jfreechart.getPlot();
        DateAxis dateaxis = (DateAxis)xyplot.getDomainAxis();
        dateaxis.setVerticalTickLabels(true);
        XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer)xyplot.getRenderer();
        xylineandshaperenderer.setBaseShapesVisible(true);
        xylineandshaperenderer.setSeriesFillPaint(0, Color.red);
        xylineandshaperenderer.setSeriesFillPaint(1, Color.white);
        xylineandshaperenderer.setUseFillPaint(true);
        xylineandshaperenderer.setLegendItemToolTipGenerator(new StandardXYSeriesLabelGenerator("Tooltip {0}"));
        BufferedImage bi = jfreechart.createBufferedImage(width, height);
        return bi;
    }

    private static BufferedImage createCountImg(long[] timestamps,  double[] count, final RrdDb rrdDb, int width, int height)
    {
        TimeSeries countTs = new TimeSeries("count");
        for (int i=0;i<timestamps.length; i++) {
            FixedMillisecond ts = new FixedMillisecond(timestamps[i] *1000);
            countTs.add(ts, count[i]);
        }
        TimeSeriesCollection timeseriescollection = new TimeSeriesCollection();
        timeseriescollection.addSeries(countTs);
        JFreeChart jfreechart = ChartFactory.createTimeSeriesChart(null, 
                "Time", "Value", timeseriescollection, true, true, false);
        XYPlot xyplot = (XYPlot)jfreechart.getPlot();
        DateAxis dateaxis = (DateAxis)xyplot.getDomainAxis();
        dateaxis.setVerticalTickLabels(true);
        XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer)xyplot.getRenderer();
        xylineandshaperenderer.setBaseShapesVisible(true);
        xylineandshaperenderer.setSeriesFillPaint(0, Color.red);
        xylineandshaperenderer.setSeriesFillPaint(1, Color.white);
        xylineandshaperenderer.setUseFillPaint(true);
        xylineandshaperenderer.setLegendItemToolTipGenerator(new StandardXYSeriesLabelGenerator("Tooltip {0}"));
        BufferedImage bi = jfreechart.createBufferedImage(width, height);
        return bi;
    }

    
    
    private static String generateHeatChart(long startTimeMillis, long endTimeMillis, final RrdDb rrdDb,
            int width, int height) throws IOException {
        FetchRequest request = rrdDb.createFetchRequest(ConsolFun.FIRST, startTimeMillis / 1000,
                endTimeMillis / 1000);
        final FetchData data = request.fetchData();
        final RrdXYZDataset dataSet = new RrdXYZDataset(data);
        File rrdFile = new File(rrdDb.getPath());
        NumberAxis xAxis = new NumberAxis("Time");


        xAxis.setStandardTickUnits(dataSet.createXTickUnits());
        xAxis.setLowerMargin(0);
        xAxis.setUpperMargin(0);
        NumberAxis yAxis = new NumberAxis("Measurement");

        yAxis.setStandardTickUnits(dataSet.createYTickUnits());
        yAxis.setLowerMargin(0);
        yAxis.setUpperMargin(0);
        XYBlockRenderer renderer = new XYBlockRenderer();
        PaintScale scale;
        if (dataSet.getMinValue() >= dataSet.getMaxValue()) {
            if (dataSet.getMinValue() == Double.POSITIVE_INFINITY) {
                scale = new InverseGrayScale(0, 1);
            } else {
                scale = new InverseGrayScale(dataSet.getMinValue(), dataSet.getMaxValue() + 1);
            }
        } else {
            scale = new InverseGrayScale(dataSet.getMinValue(), dataSet.getMaxValue());
        }

        renderer.setPaintScale(scale);
        renderer.setBlockWidth(1);
        renderer.setBlockHeight(1);

        XYPlot plot = new XYPlot(dataSet, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setRangeMinorGridlinesVisible(false);


        JFreeChart chart = new JFreeChart(rrdFile.getName(), plot);
        PaintScaleLegend legend = new PaintScaleLegend(scale, new NumberAxis("Count"));
        legend.setMargin(0, 5, 0, 5);
        chart.addSubtitle(legend);
        chart.removeLegend();
        chart.setBackgroundPaint(Color.white);
        BufferedImage bi = chart.createBufferedImage(width, height);
        File graphicFile = File.createTempFile(rrdFile.getName(), ".png",
                new File(rrdDb.getCanonicalPath()).getParentFile());
        ImageIO.write(bi, "png", graphicFile);
        return graphicFile.getPath();
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
        List<String> result = new ArrayList<String>();

        for (RrdDb db : databases.asMap().values()) {
            syncDb(db);
            FetchRequest request = db.createFetchRequest(ConsolFun.FIRST, db.getRrdDef().getStartTime(),
                    System.currentTimeMillis() / 1000);
            FetchData data = request.fetchData();
            result.add(data.dump());
        }

        return result;
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

    @Override
    public void alocateMeasurements(EntityMeasurementsInfo measurement, int sampleTimeMillis) throws IOException {
        RrdDb rrdDb = databases.getUnchecked(new Pair(measurement, msToS(sampleTimeMillis)));
        LOG.debug("Prepared rrd database: {}", rrdDb.getPath());
    }
}
