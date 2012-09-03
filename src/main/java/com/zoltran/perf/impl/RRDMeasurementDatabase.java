/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zoltran.base.Pair;
import com.zoltran.perf.EntityMeasurements;
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
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.*;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@ThreadSafe
public class RRDMeasurementDatabase implements MeasurementDatabase, Closeable, RRDMeasurementDatabaseMBean {

    private final String databaseFolder;
    private final LoadingCache<Pair<EntityMeasurements, Integer>, RrdDb> databases;
    private static final Logger LOG = LoggerFactory.getLogger(RRDMeasurementDatabase.class);

    public RRDMeasurementDatabase(final String databaseFolder) {
        this.databaseFolder = databaseFolder;
        databases = CacheBuilder.newBuilder().maximumSize(2048).build(
                new CacheLoader<Pair<EntityMeasurements, Integer>, RrdDb>() {
                    @Override
                    public RrdDb load(Pair<EntityMeasurements, Integer> key) throws Exception {
                        int sampleTimeSeconds = key.getSecond();
                        EntityMeasurements emeasurements = key.getFirst();
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

                            Map<String, Number> measurements = emeasurements.getMeasurements(false);
                            for (String mName : measurements.keySet()) {
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

    private static String getDBName(EntityMeasurements measurement, int sampleTimeSeconds) {
        return measurement.getMeasuredEntity().toString() + "_" + sampleTimeSeconds + "_"
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
        RrdDb rrdDb = databases.getUnchecked(new Pair(measurement, msToS(sampleTimeMillis)));
        Sample sample = rrdDb.createSample(msToS(timeStampMillis));
        Map<String, Number> measurements = measurement.getMeasurements(true);

        for (Map.Entry<String, Number> entry : measurements.entrySet()) {
            sample.setValue(entry.getKey(), entry.getValue().doubleValue());
        }
        try {
            sample.update();
            LOG.debug("Measurement {} persisted at {}", measurement.getMeasuredEntity(), timeStampMillis);
        } catch (IOException e) {
            throw new IOException("Cannot persist sample " + measurement.getMeasuredEntity() + " at " + timeStampMillis, e);
        } catch (RuntimeException e) {
            throw new IOException("Cannot persist sample " + measurement.getMeasuredEntity() + " at " + timeStampMillis, e);
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
    public List<String> generateCharts(long startTimeMillis, long endTimeMillis,
            int width, int height) throws IOException {
        List<String> result = new ArrayList<String>();

        for (Map.Entry<Pair<EntityMeasurements, Integer>, RrdDb> entry : databases.asMap().entrySet()) {
            int sampleTimeSeconds = entry.getKey().getSecond();
            EntityMeasurements emeasurements = entry.getKey().getFirst();
            String rrdpath = databaseFolder + File.separator + getDBName(emeasurements,
                    sampleTimeSeconds);
            syncDb(entry.getValue());
            result.add(generateMinMaxAvgChart(startTimeMillis, endTimeMillis, rrdpath, width, height));
            result.add(generateHeatChart(startTimeMillis, endTimeMillis, entry.getValue(), width, height));
        }

        LOG.info("Generated charts {}", result);
        return result;
    }

    private static String generateMinMaxAvgChart(long startTimeMillis, long endTimeMillis, String rrdpath,
            int width, int height) throws IOException {
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setWidth(width);
        gDef.setHeight(height);
        File rrdFile = new File(rrdpath);
        String graphicFile = File.createTempFile(rrdFile.getName(), ".png",
                new File(rrdpath).getParentFile()).getPath();
        gDef.setFilename(graphicFile);
        gDef.setStartTime(startTimeMillis / 1000);
        gDef.setEndTime(endTimeMillis / 1000);
        gDef.setTitle("Measurements for " + rrdpath);
        gDef.setVerticalLabel("ms");

        gDef.datasource("min", rrdpath, "min", ConsolFun.FIRST);
        gDef.datasource("max", rrdpath, "max", ConsolFun.FIRST);
        gDef.datasource("total", rrdpath, "total", ConsolFun.FIRST);
        gDef.datasource("count", rrdpath, "count", ConsolFun.FIRST);

        gDef.datasource("avg", "total,count,/");

        gDef.line("min", Color.GREEN, "min");
        gDef.line("max", Color.BLUE, "max");
        gDef.line("avg", Color.MAGENTA, "avg");
        gDef.hrule(2568, Color.GREEN, "hrule");
        gDef.setImageInfo("<img src='%s' width='%d' height = '%d'>");
        gDef.setPoolUsed(false);
        gDef.setImageFormat("png");
        if (endTimeMillis - startTimeMillis < 360000) {
            gDef.setTimeAxis(RrdGraphConstants.MINUTE, 1,
                    RrdGraphConstants.MINUTE, 30,
                    RrdGraphConstants.HOUR, 1,
                    0, "%H");
        } else {
            gDef.setTimeAxis(RrdGraphConstants.HOUR, 1,
                    RrdGraphConstants.HOUR, 24,
                    RrdGraphConstants.HOUR, 4,
                    0, "%H");
        }
        new RrdGraph(gDef);
        return graphicFile;
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
}
