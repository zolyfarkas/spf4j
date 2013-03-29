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
package org.spf4j.perf.impl.chart;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
import org.spf4j.base.Arrays;

/**
 *
 * @author zoly
 */
public final class Charts {

    private Charts() {
    }

    public static JFreeChart createHeatJFreeChart(String[] dsNames, double[][] values, long startTimeMillis, long stepMillis, String uom, String chartName) {
        final QuantizedXYZDatasetImpl dataSet = new QuantizedXYZDatasetImpl(dsNames, values, startTimeMillis, stepMillis);
        NumberAxis xAxis = new NumberAxis("Time");
        xAxis.setStandardTickUnits(dataSet.createXTickUnits());
        xAxis.setLowerMargin(0);
        xAxis.setUpperMargin(0);
        NumberAxis yAxis = new NumberAxis(uom);
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
        JFreeChart chart = new JFreeChart(chartName, plot);
        PaintScaleLegend legend = new PaintScaleLegend(scale, new NumberAxis("Count"));
        legend.setMargin(0, 5, 0, 5);
        chart.addSubtitle(legend);
        chart.removeLegend();
        chart.setBackgroundPaint(Color.white);
        return chart;
    }

    private static BufferedImage createChartImg(String chartName, String uom, TimeSeriesCollection timeseriescollection, int width, int height) {
        JFreeChart jfreechart = createJFreeChart(chartName, uom, timeseriescollection);
        BufferedImage bi = jfreechart.createBufferedImage(width, height);
        return bi;
    }

    private static JFreeChart createJFreeChart(String chartName, String uom, TimeSeriesCollection timeseriescollection) {
        JFreeChart jfreechart = ChartFactory.createTimeSeriesChart(chartName,
                "Time", uom, timeseriescollection, true, true, false);
        XYPlot xyplot = (XYPlot) jfreechart.getPlot();
        DateAxis dateaxis = (DateAxis) xyplot.getDomainAxis();
        dateaxis.setVerticalTickLabels(true);
        XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyplot.getRenderer();
        xylineandshaperenderer.setBaseShapesVisible(true);
        xylineandshaperenderer.setUseFillPaint(true);
        xylineandshaperenderer.setLegendItemToolTipGenerator(new StandardXYSeriesLabelGenerator("Tooltip {0}"));
        return jfreechart;
    }

    public static BufferedImage createMinMaxAvgCountImg(String chartName, long[] timestamps,
            double[] min, double[] max, double[] total, double[] count, String uom, int width, int height) {

        BufferedImage bi = Charts.createTimeSeriesChartImg(chartName, timestamps,
                new String[]{"min", "max", "avg"}, uom, new double[][]{min, max, Arrays.divide(total, count)}, width, height - height / 3);
        BufferedImage bi2 = Charts.createTimeSeriesChartImg(null, timestamps,
                new String[]{"count"}, "count", new double[][]{count}, width, height / 3);
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        combined.getGraphics().drawImage(bi, 0, 0, null);
        combined.getGraphics().drawImage(bi2, 0, height - height / 3, null);
        return combined;
    }

    public static BufferedImage createTimeSeriesChartImg(String chartName, long[] timestamps,
            String[] measurementNames, String uom, double[][] measurements, int width, int height) {
        TimeSeriesCollection timeseriescollection = new TimeSeriesCollection();
        for (int i = 0; i < measurementNames.length; i++) {
            TimeSeries tseries = new TimeSeries(measurementNames[i]);
            for (int j = 0; j < timestamps.length; j++) {
                FixedMillisecond ts = new FixedMillisecond(timestamps[j]);
                tseries.add(ts, measurements[i][j]);
            }
            timeseriescollection.addSeries(tseries);
        }
        BufferedImage bi = createChartImg(chartName, uom, timeseriescollection, width, height);
        return bi;
    }

    public static BufferedImage createTimeSeriesChartImg(String chartName, long[][] timestamps,
            String[] measurementNames, String uom, double[][] measurements, int width, int height) {
        TimeSeriesCollection timeseriescollection = new TimeSeriesCollection();
        for (int i = 0; i < measurementNames.length; i++) {
            TimeSeries tseries = new TimeSeries(measurementNames[i]);
            for (int j = 0; j < timestamps[i].length; j++) {
                FixedMillisecond ts = new FixedMillisecond(timestamps[i][j]);
                tseries.add(ts, measurements[i][j]);
            }
            timeseriescollection.addSeries(tseries);
        }
        BufferedImage bi = createChartImg(chartName, uom, timeseriescollection, width, height);
        return bi;
    }

    public static BufferedImage createHeatChartImg(String chartName, String uom,
            String[] dsNames, double[][] values,
            long startTimeMillis, long stepMillis, int width, int height) throws IOException {
        JFreeChart chart = createHeatJFreeChart(dsNames, values, startTimeMillis, stepMillis, uom, chartName);
        return chart.createBufferedImage(width, height);
    }
}
