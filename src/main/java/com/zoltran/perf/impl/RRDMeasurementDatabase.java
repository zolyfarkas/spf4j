/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf.impl;

import com.zoltran.base.Pair;
import com.zoltran.perf.EntityMeasurements;
import com.zoltran.perf.MeasurementDatabase;
import com.zoltran.perf.impl.QuantizedRecorder.Quanta;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.ThreadSafe;
import javax.imageio.ImageIO;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.*;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphDef;

/**
 *
 * @author zoly
 */
@ThreadSafe
public class RRDMeasurementDatabase implements MeasurementDatabase, Closeable {

    private final String databaseFolder;
    private final Map<String, RrdDb> databases;

    public RRDMeasurementDatabase(String databaseFolder) {
        this.databaseFolder = databaseFolder;
        databases = new HashMap<String, RrdDb>();
    }

    private static String getDBName(EntityMeasurements measurement, int sampleTimeSeconds) {
        return measurement.getMeasuredEntity().toString() + "_" + sampleTimeSeconds + "_"
                + measurement.getUnitOfMeasurement() + "_" + new LocalDate().getWeekOfWeekyear() + ".rrd4j";
    }
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_WEEK = 3600 * 24 * 7;

    public RrdDb getRrdDb(EntityMeasurements measurement, int sampleTimeSeconds) throws IOException {
        String rrdFilePath = databaseFolder + File.separator + getDBName(measurement, sampleTimeSeconds);
        synchronized (databases) {
            RrdDb result = databases.get(rrdFilePath);
            if (result == null) {
                File rrdFile = new File(rrdFilePath);
                if (rrdFile.exists()) {
                    result = new RrdDb(rrdFilePath);
                } else {
                    RrdDef rrdDef = new RrdDef(rrdFilePath, sampleTimeSeconds);
                    rrdDef.addArchive(ConsolFun.FIRST, 0.5, 1, SECONDS_PER_WEEK / sampleTimeSeconds); // 1 week worth of data at original granularity.
                    rrdDef.setStartTime(System.currentTimeMillis() / 1000);
                    int heartbeat = sampleTimeSeconds * 2;

                    Map<String, Number> measurements = measurement.getMeasurements(false);
                    for (String mName : measurements.keySet()) {
                        rrdDef.addDatasource(mName, DsType.GAUGE, heartbeat, Double.NaN, Double.NaN);
                    }
                    result = new RrdDb(rrdDef);
                }
                databases.put(rrdFilePath, result);
            }
            return result;
        }
    }

    public void saveMeasurements(EntityMeasurements measurement, long timeStampMillis, int sampleTimeMillis) throws IOException {
        System.out.println("time: " + timeStampMillis + "m = " + measurement);
        RrdDb rrdDb = getRrdDb(measurement, sampleTimeMillis / 1000);
        Sample sample = rrdDb.createSample(timeStampMillis / 1000);
        Map<String, Number> measurements = measurement.getMeasurements(true);

        for (Map.Entry<String, Number> entry : measurements.entrySet()) {
            sample.setValue(entry.getKey(), entry.getValue().doubleValue());
        }
        sample.update();
    }

    @PreDestroy
    public void close() throws IOException {
        synchronized (databases) {
            for (RrdDb db : databases.values()) {
                db.close();
            }
            databases.clear();
        }
    }

    public List<String> generateCharts(long startTimeMillis, long endTimeMillis, int width, int height) throws IOException {
        List<String> result = new ArrayList<String>();
        synchronized (databases) {
            for (Map.Entry<String, RrdDb> entry : databases.entrySet()) {
                String rrdpath = entry.getKey();
                syncDb(entry.getValue());
                result.add(generateMinMaxAvgChart( startTimeMillis, endTimeMillis, rrdpath, width, height));
                result.add(generateHeatChart(startTimeMillis, endTimeMillis, entry.getValue(), width, height));
            }
        }
        return result;
    }

    private static String generateMinMaxAvgChart(long startTimeMillis, long endTimeMillis, String rrdpath,
            int width, int height) throws IOException {
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setWidth(width);
        gDef.setHeight(height);
        File rrdFile = new File(rrdpath);
        String graphicFile = File.createTempFile(rrdFile.getName(), ".png").getPath();       
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
        if (endTimeMillis -startTimeMillis < 360000) {
             gDef. setTimeAxis(RrdGraphConstants.MINUTE, 1,
             RrdGraphConstants.MINUTE, 30,
             RrdGraphConstants.HOUR, 1,
             0, "%H");
        }
        else {    
            gDef. setTimeAxis(RrdGraphConstants.HOUR, 1,
             RrdGraphConstants.HOUR, 24,
             RrdGraphConstants.HOUR, 4,
             0, "%H");
        }
        new RrdGraph(gDef);
        return graphicFile;
    }
    
    private static String generateHeatChart( long startTimeMillis, long endTimeMillis, final RrdDb rrdDb,
            int width, int height) throws IOException {
        FetchRequest request = rrdDb.createFetchRequest(ConsolFun.FIRST, startTimeMillis/1000,
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
        if (dataSet.getMinValue() >= dataSet.getMaxValue())
            if (dataSet.getMinValue() == Double.POSITIVE_INFINITY)
                scale = new InverseGrayScale(0, 1);
            else
                scale = new InverseGrayScale(dataSet.getMinValue(), dataSet.getMaxValue()+1);
        else
            scale = new InverseGrayScale(dataSet.getMinValue(), dataSet.getMaxValue()) ;
            
        renderer.setPaintScale(scale);
        renderer.setBlockWidth(1);
        renderer.setBlockHeight(1);
       
        XYPlot plot = new XYPlot(dataSet, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setRangeMinorGridlinesVisible(false);
        
       
        JFreeChart chart = new JFreeChart(rrdFile.getName(), plot);
        PaintScaleLegend legend = new PaintScaleLegend(scale,new NumberAxis("Count"));
        legend.setMargin(0, 5, 0, 5);
        chart.addSubtitle(legend);
        chart.removeLegend();
        chart.setBackgroundPaint(Color.white);
        BufferedImage bi = chart.createBufferedImage(width, height);
        File graphicFile = File.createTempFile(rrdFile.getName(), ".png"); 
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

    public List<String> getMeasurements() throws IOException {
        List<String> result = new ArrayList<String>();
        synchronized (databases) {
            for (Map.Entry<String, RrdDb> entry : databases.entrySet()) {
                RrdDb db = entry.getValue();
                syncDb(db);
                FetchRequest request = db.createFetchRequest(ConsolFun.FIRST, db.getRrdDef().getStartTime(),
                        System.currentTimeMillis() / 1000);
                FetchData data = request.fetchData();
                result.add(data.dump());
            }
        }
        return result;
    }

    public List<String> generate(Properties props) throws IOException {
       int width = Integer.valueOf(props.getProperty("width", "1200"));
       int height = Integer.valueOf(props.getProperty("height", "800"));
       long startTime = Long.valueOf(props.getProperty("startTime",
               Long.toString( new LocalDate().withDayOfWeek(DateTimeConstants.MONDAY).toDate().getTime())  ));
       long endTime = Long.valueOf(props.getProperty("endTime", Long.toString(System.currentTimeMillis())));
       return generateCharts(startTime, endTime, width, height);
    }
    

    public List<String> getParameters() {
        return Arrays.asList("width", "height", "startTime", "endTime");
    }
}
