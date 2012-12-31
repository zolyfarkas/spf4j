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

import com.zoltran.base.ComparablePair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYZDataset;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.rrd4j.core.FetchData;

@Immutable
@edu.umd.cs.findbugs.annotations.SuppressWarnings("SE_BAD_FIELD_INNER_CLASS")
public class RrdXYZDataset implements XYZDataset {

    private final double[] x;
    private final double[] y;
    private final double[] z;
    private final double minValue;
    private final double maxValue;
    private final List<ComparablePair<Quanta, Integer>> quantas;
    private final FetchData data;

    public RrdXYZDataset(final FetchData data) {
        this.data = data;
        String[] dataSources = data.getDsNames();
        quantas = new ArrayList<ComparablePair<Quanta, Integer>>();
        for (int i = 0; i < dataSources.length; i++) {
            String ds = dataSources[i];
            if (ds.startsWith("Q")) {
                Quanta quanta = new Quanta(ds);
                quantas.add(new ComparablePair(quanta, i));
            }
        }
        Collections.sort(quantas);
        int seriesSize = quantas.size() * data.getValues(quantas.get(0).getSecond()).length;
        x = new double[seriesSize];
        y = new double[seriesSize];
        z = new double[seriesSize];
        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;

        int k = 0;

        //long [] timestamps = data.getTimestamps();        
        for (int j = 0; j < quantas.size(); j++) {
            ComparablePair<Quanta, Integer> pair = quantas.get(j);
            double[] values = data.getValues(pair.getSecond());

            for (int i = 0; i < values.length; i++) {
                x[k] = i; //timestamps[i]*1000;
                y[k] = j; //(double) pair.getFirst().getClosestToZero();
                double zval = values[i];
                z[k] = zval;
                if (zval > maxValue) {
                    maxValue = zval;
                }
                if (zval < minValue) {
                    minValue = zval;
                }
                k++;
            }
        }
        this.minValue = minValue;
        this.maxValue = maxValue;

    }

    @Override
    public Number getZ(int series, int item) {
        return z[item];
    }

    @Override
    public double getZValue(int series, int item) {
        return z[item];
    }

    @Override
    public DomainOrder getDomainOrder() {
        return DomainOrder.ASCENDING;
    }

    @Override
    public int getItemCount(int series) {
        return x.length;
    }

    @Override
    public Number getX(int series, int item) {
        return x[item];
    }

    @Override
    public double getXValue(int series, int item) {
        return x[item];
    }

    @Override
    public Number getY(int series, int item) {
        return y[item];
    }

    @Override
    public double getYValue(int series, int item) {
        return y[item];
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public Comparable getSeriesKey(int series) {
        return "RrdXYZDataset";
    }

    @Override
    public int indexOf(Comparable seriesKey) {
        return 0;
    }

    @Override
    public void addChangeListener(DatasetChangeListener listener) {
        // nothing
    }

    @Override
    public void removeChangeListener(DatasetChangeListener listener) {
        // nothing
    }

    @Override
    public DatasetGroup getGroup() {
        return null;
    }

    @Override
    public void setGroup(DatasetGroup group) {
        // nothing
    }

    public double getMaxValue() {
        return maxValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public List<ComparablePair<Quanta, Integer>> getQuantas() {
        return quantas;
    }

    public FetchData getData() {
        return data;
    }

    public TickUnits createXTickUnits() {
        TickUnits tux = new TickUnits();
        final DateTimeFormatter formatter = ISODateTimeFormat.dateHourMinuteSecond();
        final DateTimeFormatter shortFormat = ISODateTimeFormat.dateHour();
        final DateTimeFormatter mediumFormat = ISODateTimeFormat.dateHourMinute();
        final long[] timestamps = data.getTimestamps();
        tux.add(new NumberTickUnitImpl(1, timestamps, formatter)); // base
        long nr = 5 / data.getStep();
        if (nr > 1) {
            tux.add(new NumberTickUnitImpl(nr, timestamps, formatter));
        }
        
        nr = 15 / data.getStep();
        if (nr > 1) {
            tux.add(new NumberTickUnitImpl(nr, timestamps, formatter));
        }
        // minute
        nr = 60 / data.getStep();
        if (nr > 1) {
            tux.add(new NumberTickUnitImpl(nr, timestamps, mediumFormat));
        }
        // 15 minute
        nr = 900 /data.getStep();
        if (nr > 1) {
            tux.add(new NumberTickUnitImpl(nr, timestamps, mediumFormat));
        }
        // hour
        nr =3600 /data.getStep();
        if (nr> 1) {
            tux.add(new NumberTickUnitImpl(nr, timestamps, shortFormat));
        }
        // 6 hour
        nr = 21600 /data.getStep();
        if (nr > 1) {
            tux.add(new NumberTickUnitImpl(nr, timestamps, shortFormat));
        }

        return tux;
    }

    public TickUnits createYTickUnits() {
        TickUnits tu = new TickUnits();
        final List<ComparablePair<Quanta, Integer>> lquantas = this.getQuantas();
        tu.add(new NumberTickUnit(1) {

            @Override
            public String valueToString(double value) {
                long val = lquantas.get((int) Math.round(value)).getFirst().getIntervalStart();
                if (val == Long.MIN_VALUE) {
                    return "NI";
                } else {
                    return Long.toString(val);
                }
            }
        });
        return tu;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    private class NumberTickUnitImpl extends NumberTickUnit {

        private final long[] timestamps;
        private final DateTimeFormatter formatter;

        public NumberTickUnitImpl(double size, long[] timestamps, DateTimeFormatter formatter) {
            super(size);
            this.timestamps = timestamps;
            this.formatter = formatter;
        }

        @Override
        public String valueToString(double value) {
            int ival = (int) Math.round(value);
            long val;
            if (ival >= timestamps.length) {
                val = timestamps[timestamps.length - 1] + data.getStep() * (ival - timestamps.length + 1);
            } else {
                val = timestamps[ival];
            }
            return formatter.print(val * 1000);
        }
        
        
    }
}
