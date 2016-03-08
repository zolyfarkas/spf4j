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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.jfree.chart.axis.TickUnits;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.XYZDataset;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.spf4j.base.Arrays;
import org.spf4j.base.ComparablePair;
import org.spf4j.perf.impl.Quanta;

@Immutable
public final class QuantizedXYZDatasetImpl implements XYZDataset, Serializable {

    private final double[] x;
    private final double[] y;
    private final double[] z;
    private final double minValue;
    private final double maxValue;
    private final ArrayList<ComparablePair<Quanta, Integer>> quantas;
    private final double[][] data;
    private final long startTimeMillis;
    private final long stepMillis;

    public QuantizedXYZDatasetImpl(final String[] dataSources, final double[][] pdata,
                        final long startTimeMillis, final long step) {
        this.data = pdata.clone();
        this.startTimeMillis = startTimeMillis;
        this.stepMillis = step;
        quantas = new ArrayList<>(dataSources.length);
        for (int i = 0; i < dataSources.length; i++) {
            String ds = dataSources[i];
            if (ds.startsWith("Q")) {
                Quanta quanta = new Quanta(ds);
                quantas.add(ComparablePair.of(quanta, i));
            }
        }
        Collections.sort(quantas);
        final int nrQuantas = quantas.size();
        int seriesSize = nrQuantas * data.length;
        x = new double[seriesSize];
        y = new double[seriesSize];
        z = new double[seriesSize];
        double lMinValue = Double.POSITIVE_INFINITY;
        double lMaxValue = Double.NEGATIVE_INFINITY;

        int k = 0;
      
        for (int j = 0; j < nrQuantas; j++) {
            ComparablePair<Quanta, Integer> pair = quantas.get(j);
            double[] values = Arrays.getColumn(data, pair.getSecond());
            for (int i = 0; i < values.length; i++) {
                x[k] = i; //timestamps[i]*1000;
                y[k] = j; //(double) pair.getFirst().getClosestToZero();
                double zval = values[i];
                z[k] = zval;
                if (zval > lMaxValue) {
                    lMaxValue = zval;
                }
                if (zval < lMinValue) {
                    lMinValue = zval;
                }
                k++;
            }
        }
        this.minValue = lMinValue;
        this.maxValue = lMaxValue;

    }

    @Override
    public Number getZ(final int series, final int item) {
        return z[item];
    }

    @Override
    public double getZValue(final int series, final int item) {
        return z[item];
    }

    @Override
    public DomainOrder getDomainOrder() {
        return DomainOrder.ASCENDING;
    }

    @Override
    public int getItemCount(final int series) {
        return x.length;
    }

    @Override
    public Number getX(final int series, final int item) {
        return x[item];
    }

    @Override
    public double getXValue(final int series, final int item) {
        return x[item];
    }

    @Override
    public Number getY(final int series, final int item) {
        return y[item];
    }

    @Override
    public double getYValue(final int series, final int item) {
        return y[item];
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public Comparable getSeriesKey(final int series) {
        return "RrdXYZDataset";
    }

    @Override
    public int indexOf(final Comparable seriesKey) {
        return 0;
    }

    @Override
    public void addChangeListener(final DatasetChangeListener listener) {
        // nothing
    }

    @Override
    public void removeChangeListener(final DatasetChangeListener listener) {
        // nothing
    }

    @Override
    public DatasetGroup getGroup() {
        return null;
    }

    @Override
    public void setGroup(final DatasetGroup group) {
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

 

    public TickUnits createXTickUnits() {
        TickUnits tux = new TickUnits();
        if (data.length == 0) {
            return tux;
        }
        
        final DateTimeFormatter formatter = ISODateTimeFormat.dateHourMinuteSecond();
        final DateTimeFormatter shortFormat = ISODateTimeFormat.dateHour();
        final DateTimeFormatter mediumFormat = ISODateTimeFormat.dateHourMinute();
        final long[] timestamps = new long[data[0].length];
        long time = startTimeMillis;
        for (int i = 0; i < timestamps.length; i++) {
            timestamps[i] = time;
            time += stepMillis;
        }
        tux.add(new TimestampTickUnitImpl(1, timestamps, stepMillis, formatter)); // base
        long nr = 5000L / stepMillis;
        if (nr > 1) {
            tux.add(new TimestampTickUnitImpl(nr, timestamps, stepMillis, formatter));
        }
        
        nr = 15000L / stepMillis;
        if (nr > 1) {
            tux.add(new TimestampTickUnitImpl(nr, timestamps, stepMillis, formatter));
        }
        // minute
        nr = 60000L / stepMillis;
        if (nr > 1) {
            tux.add(new TimestampTickUnitImpl(nr, timestamps, stepMillis, mediumFormat));
        }
        // 15 minute
        nr = 900000L / stepMillis;
        if (nr > 1) {
            tux.add(new TimestampTickUnitImpl(nr, timestamps, stepMillis, mediumFormat));
        }
        // hour
        nr = 3600000L / stepMillis;
        if (nr > 1) {
            tux.add(new TimestampTickUnitImpl(nr, timestamps, stepMillis, shortFormat));
        }
        // 6 hour
        nr = 21600000L / stepMillis;
        if (nr > 1) {
            tux.add(new TimestampTickUnitImpl(nr, timestamps, stepMillis, shortFormat));
        }

        return tux;
    }

    public TickUnits createYTickUnits() {
        TickUnits tu = new TickUnits();
        final List<ComparablePair<Quanta, Integer>> lquantas = this.getQuantas();
        tu.add(new QuantizedNumberTickUnit(1, lquantas));
        return tu;
    }

    
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return "QuantizedXYZDatasetImpl{" + "x=" + java.util.Arrays.toString(x)
                + ", y=" + java.util.Arrays.toString(y) + ", z=" + java.util.Arrays.toString(z)
                + ", minValue=" + minValue + ", maxValue=" + maxValue + ", quantas=" + quantas
                + ", data=" + java.util.Arrays.toString(data)
                + ", startTimeMillis=" + startTimeMillis + ", stepMillis=" + stepMillis + '}';
    }

    

}
