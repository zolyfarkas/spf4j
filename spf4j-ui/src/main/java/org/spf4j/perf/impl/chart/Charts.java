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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
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
import org.jfree.data.xy.XYDataset;
import org.spf4j.base.Arrays;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
public final class Charts {

    private Charts() {
    }

    public static JFreeChart createHeatJFreeChart(final String[] dsNames, final double[][] values,
            final long startTimeMillis, final long stepMillis, final String uom, final String chartName) {
        final QuantizedXYZDatasetImpl dataSet = new QuantizedXYZDatasetImpl(dsNames, values,
                startTimeMillis, stepMillis);
        NumberAxis xAxis = new NumberAxis("Time");
        xAxis.setStandardTickUnits(dataSet.createXTickUnits());
        xAxis.setLowerMargin(0);
        xAxis.setUpperMargin(0);
        xAxis.setVerticalTickLabels(true);
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

    private static JFreeChart createJFreeChart(final String chartName, final String uom,
            final XYDataset timeseriescollection) {
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

    public static BufferedImage createMinMaxAvgCountImg(final String chartName, final long[] timestamps,
            final double[] min, final double[] max, final double[] total, final double[] count,
            final String uom, final int width, final int height) {

        BufferedImage bi = Charts.createTimeSeriesJFreeChart(chartName, timestamps,
                new String[]{"min", "max", "avg"}, uom,
                new double[][]{min, max, Arrays.divide(total, count)}).createBufferedImage(width, height - height / 3);
        BufferedImage bi2 = Charts.createTimeSeriesJFreeChart(null, timestamps,
                new String[]{"count"}, "count", new double[][]{count}).createBufferedImage(width, height / 3);
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics graphics = combined.getGraphics();
        try {
            graphics.drawImage(bi, 0, 0, null);
            graphics.drawImage(bi2, 0, height - height / 3, null);
        } finally {
            graphics.dispose();
        }
        return combined;
    }

    public static BufferedImage generateCountTotalChart(final String groupName, final long[][] timestamps,
            final String[] measurementNames, final String uom1, final double[][] measurements, final int width,
            final int height, final String[] measurementNames2, final String uom2, final double[][] measurements2) {
        BufferedImage count = Charts.createTimeSeriesJFreeChart("Measurements for "
                + groupName + " generated by spf4j",
                timestamps, measurementNames, uom1, measurements).createBufferedImage(width, height / 2);
        BufferedImage total = Charts.createTimeSeriesJFreeChart(null,
                timestamps, measurementNames2, uom2, measurements2).createBufferedImage(width, height / 2);
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics graphics = combined.getGraphics();
        try {
            graphics.drawImage(count, 0, 0, null);
            graphics.drawImage(total, 0, height / 2, null);
        } finally {
            graphics.dispose();
        }
        return combined;
    }



    private static TimeSeriesCollection createTimeSeriesCollection(final String[] measurementNames,
            final long[] timestamps, final double[][] measurements) {
        TimeSeriesCollection timeseriescollection = new TimeSeriesCollection();
        for (int i = 0; i < measurementNames.length; i++) {
            TimeSeries tseries = new TimeSeries(measurementNames[i]);
            for (int j = 0; j < timestamps.length; j++) {
                FixedMillisecond ts = new FixedMillisecond(timestamps[j]);
                tseries.add(ts, measurements[i][j]);
            }
            timeseriescollection.addSeries(tseries);
        }
        return timeseriescollection;
    }




    public static JFreeChart createTimeSeriesJFreeChart(final String chartName, final long[] timestamps,
            final String[] measurementNames, final String uom, final double[][] measurements) {
        TimeSeriesCollection timeseriescollection =
                createTimeSeriesCollection(measurementNames, timestamps, measurements);
        return createJFreeChart(chartName, uom, timeseriescollection);
    }


    private static TimeSeriesCollection createTimeSeriesCollection(final String[] measurementNames,
            final long[][] timestamps, final double[][] measurements) {
        TimeSeriesCollection timeseriescollection = new TimeSeriesCollection();
        for (int i = 0; i < measurementNames.length; i++) {
            TimeSeries tseries = new TimeSeries(measurementNames[i]);
            for (int j = 0; j < timestamps[i].length; j++) {
                FixedMillisecond ts = new FixedMillisecond(timestamps[i][j]);
                tseries.add(ts, measurements[i][j]);
            }
            timeseriescollection.addSeries(tseries);
        }
        return timeseriescollection;
    }



    public static JFreeChart createTimeSeriesJFreeChart(final String chartName, final long[][] timestamps,
            final String[] measurementNames, final String uom, final double[][] measurements) {
        TimeSeriesCollection timeseriescollection =
                createTimeSeriesCollection(measurementNames, timestamps, measurements);
        return createJFreeChart(chartName, uom, timeseriescollection);
    }




    public static Pair<long[], double[][]> fillGaps(final long[] timestamps,
            final long[][] data, final int sampleTime, final int nrColumns) {
        long startTime = timestamps[0];
        int nrSamples = (int) ((timestamps[timestamps.length - 1] - startTime) / sampleTime) + 1;
        long[] lts = new long[nrSamples];
        double[][] dr = new double[nrSamples][];
        long nextTime = startTime;
        int j = 0;
        int maxDeviation = sampleTime / 2;
        double[] nodata = new double[nrColumns];
        for (int i = 0; i < nrColumns; i++) {
            nodata[i] = Double.NaN;
        }
        for (int i = 0; i < nrSamples; i++) {
            lts[i] = nextTime;

            if (Math.abs(timestamps[j] - nextTime) < maxDeviation) {
                dr[i] = Arrays.toDoubleArray(data[j]);
                j++;
            } else {
                dr[i] = nodata;
            }
            nextTime += sampleTime;
        }
        return Pair.of(lts, dr);
    }


}
