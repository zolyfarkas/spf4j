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
package org.spf4j.perf.impl;

import com.google.common.math.IntMath;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.perf.MeasurementAccumulator;
import org.spf4j.perf.MeasurementsInfo;
import java.util.Arrays;
import javax.annotation.concurrent.ThreadSafe;

/**
 * inspired by DTrace LLQUANTIZE
 *
 * @author zoly
 */
@ThreadSafe
public final class QuantizedAccumulator extends AbstractMeasurementAccumulator {

  private long minMeasurement;
  private long maxMeasurement;
  private long measurementCount;
  private long measurementTotal;
  private final int quantasPerMagnitude;
  private final long[] bucketLimits;
  private final MeasurementsInfo info;
  private final int factor;
  private final int lowerMagnitude;
  private final int higherMagnitude;
  /**
   * (long.min - f ^ l), (f ^ l - f ^ (l+1)), ... (f ^(L-1) - f ^ L) (f^L - long.max)
   *
   * f = 10 m = 0 M = 5
   *
   */
  private final long[] quatizedMeasurements;

  /**
   * Create a quantized accumulator.
   * factor = 10
   * lowMagnitude = -2
   * higherMagnitude = 2
   * quantasPerMagnitude = 10
   *
   * results in -100 -90 -80 .. -10 -9 -8  .. -1 0 1 2 .. 9 10 20 30 .. 100
   *
   * @param measuredEntity - and object representing the thing we accumulate measurements for.
   * @param description - description of the thing we accumulate measurements for.
   * @param unitOfMeasurement - unit of measurement.
   * @param factor
   * @param lowerMagnitude the lower end of the quantized recording (lowerMagnitude/lowerMagnitude) * factor ^
   * |lowerMagnitude|
   * @param higherMagnitude the higher end of the quantized recording (higherMagnitude/higherMagnitude) * factor ^
   * |higherMagnitude|
   * @param quantasPerMagnitude number of buckets / magnitude
   */
  public QuantizedAccumulator(final Object measuredEntity,
          final String description,
          final String unitOfMeasurement,
          final int factor, final int lowerMagnitude,
          final int higherMagnitude, final int quantasPerMagnitude) {
    assert (quantasPerMagnitude <= factor);
    assert (lowerMagnitude < higherMagnitude);
    assert (quantasPerMagnitude > 0);
    this.factor = factor;
    this.lowerMagnitude = lowerMagnitude;
    this.higherMagnitude = higherMagnitude;
    minMeasurement = Long.MAX_VALUE;
    maxMeasurement = Long.MIN_VALUE;
    measurementCount = 0;
    measurementTotal = 0;
    this.quantasPerMagnitude = quantasPerMagnitude;
    long[] magnitudes = createMagnitudeLimits2(factor, lowerMagnitude, higherMagnitude);
    int qm1 = quantasPerMagnitude - 1;
    int l = (magnitudes.length - 1) * qm1 + 2;
    if (lowerMagnitude < 0) {
      l += 2;
    } else if (lowerMagnitude == 0) {
      l++;
    }
    this.quatizedMeasurements = new long[l];
    this.bucketLimits = new long[l - 1];
    int nrm = l + 4;
    final String[] uom = new String[nrm];
    final String[] result = new String[nrm];
    int m = 0;
    result[m] = "total";
    uom[m] = unitOfMeasurement;
    m++;
    result[m] = "count";
    uom[m] = "count";
    m++;
    result[m] = "min";
    uom[m] = unitOfMeasurement;
    m++;
    result[m] = "max";
    uom[m] = unitOfMeasurement;
    m++;
    long lm = magnitudes[0];
    result[m] = "QNI_" + lm;
    uom[m] = "count";
    m++;
    bucketLimits[0] = lm;
    if (magnitudes.length > 0) {
      int k = 1;
      long prevVal = lm;
      StringBuilder sb = new StringBuilder(16);
      for (int i = 1; i < magnitudes.length; i++) {
        long magVal = magnitudes[i];
        if (prevVal < 0) {
          long qsize = -prevVal / quantasPerMagnitude;
          int nrQ = (magVal == 0) ? quantasPerMagnitude : qm1;
          long pval = prevVal;
          for (int j = 0; j < nrQ - 1; j++) {
            sb.setLength(0);
            sb.append('Q').append(pval).append('_');
            pval += qsize;
            sb.append(pval);
            result[m] = sb.toString();
            uom[m] = "count";
            m++;
            bucketLimits[k++] = pval;
          }
          sb.setLength(0);
          sb.append('Q').append(pval).append('_');
          pval +=  magVal - pval;
          sb.append(pval);
          result[m] = sb.toString();
          uom[m] = "count";
          m++;
          bucketLimits[k++] = pval;
        } else {
            long qsize =  magVal / quantasPerMagnitude;
            int nrQ = (prevVal == 0) ? quantasPerMagnitude : qm1;
            long pval = prevVal;
            sb.setLength(0);
            sb.append('Q').append(pval).append('_');
            pval +=  qsize +  (pval == 0 ? 0 : qsize - pval);
            sb.append(pval);
            result[m] = sb.toString();
            uom[m] = "count";
            m++;
            bucketLimits[k++] = pval;
            for (int j = 0; j < nrQ - 1; j++) {
              sb.setLength(0);
              sb.append('Q').append(pval).append('_');
              pval += qsize;
              sb.append(pval);
              result[m] = sb.toString();
              uom[m] = "count";
              m++;
              bucketLimits[k++] = pval;
            }
        }
        prevVal = magVal;
      }
      sb.setLength(0);
      result[m] = sb.append('Q').append(prevVal)
              .append("_PI").toString();
      uom[m] = "count";
    }
    info = new MeasurementsInfoImpl(measuredEntity, description, result, uom);

  }

  static long[] createMagnitudeLimits(final int factor, final int lowerMagnitude,
          final int higherMagnitude) {
    long[] magnitudes = new long[(higherMagnitude - lowerMagnitude) + 1];

    int idx = 0;

    for (int m = lowerMagnitude; m <= higherMagnitude; m++) {
      if (m == 0) {
        magnitudes[idx++] = 0;
      } else {
        magnitudes[idx++] = (m < 0 ? -1 : 1) * IntMath.pow(factor, Math.abs(m));
      }
    }
    return magnitudes;
  }

  static long[] createMagnitudeLimits2(final int factor, final int lowerMagnitude,
          final int higherMagnitude) {
    long[] magnitudes = new long[(higherMagnitude - lowerMagnitude) + 1];
    int idx = 0;
    if (lowerMagnitude < 0) {  // we quantize negative values.
      int toMagnitude = Math.min(-1, higherMagnitude);
      int toValue = -IntMath.pow(factor, -toMagnitude);
      idx = toMagnitude - lowerMagnitude;
      int j = idx;
      while (j >= 0) {
        magnitudes[j--] = toValue;
        toValue *= factor;
      }
      idx++;
    }
    if (lowerMagnitude <= 0 && higherMagnitude >= 0) {
      magnitudes[idx++] = 0;
    }

    int fromMagnitude = Math.max(1, lowerMagnitude);
    int fromValue = IntMath.pow(factor, fromMagnitude);
    int j = idx;
    while (j < magnitudes.length) {
      magnitudes[j++] = fromValue;
      fromValue *= factor;
    }
    return magnitudes;
  }

  //CHECKSTYLE:OFF
  private QuantizedAccumulator(final MeasurementsInfo info, final int factor,
          final int lowerMagnitude, final int higherMagnitude,
          final long minMeasurement, final long maxMeasurement,
          final long measurementCount, final long measurementTotal,
          final int quantasPerMagnitude, final long[] bucketLimits, final long[] quatizedMeasurements) {
    //CHECKSTYLE:ON
    assert (quantasPerMagnitude <= factor);
    assert (lowerMagnitude < higherMagnitude);
    assert (quantasPerMagnitude > 0);
    this.factor = factor;
    this.lowerMagnitude = lowerMagnitude;
    this.higherMagnitude = higherMagnitude;
    this.minMeasurement = minMeasurement;
    this.maxMeasurement = maxMeasurement;
    this.measurementCount = measurementCount;
    this.measurementTotal = measurementTotal;
    this.quantasPerMagnitude = quantasPerMagnitude;
    this.bucketLimits = bucketLimits;
    this.quatizedMeasurements = quatizedMeasurements;
    this.info = info;
  }

  public String getUnitOfMeasurement() {
    return info.getMeasurementUnit(0);
  }

/**
 * bucketLimits:  -10, -5, 0, 5, 10
 * buckets: [< -10], [-10 <= x < -5], [-5 <= x < 0], [0 <= x < 5], [5 <= x < 10], [x >= 10]
 * @param bucketLimits
 * @param value
 * @return
 */
  static int findBucket(final long[] bucketLimits, final long value) {
    int idx = java.util.Arrays.binarySearch(bucketLimits, value);
    if (idx >= 0) {
      return idx + 1;
    } else {
      return -(idx + 1);
    }
  }


  @Override
  public synchronized void record(final long measurement) {
    measurementCount++;
    measurementTotal += measurement;  // TODO: check for overflow
    if (measurement < minMeasurement) {
      minMeasurement = measurement;
    }
    if (measurement > maxMeasurement) {
      maxMeasurement = measurement;
    }
    quatizedMeasurements[findBucket(bucketLimits, measurement)]++;
  }

  @Override
  @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
  public synchronized long[] get() {
    if (measurementCount == 0) {
      return null;
    } else {
      long[] result = new long[info.getNumberOfMeasurements()];
      int i = 0;
      result[i++] = this.measurementTotal;
      result[i++] = this.measurementCount;

      result[i++] = this.minMeasurement;
      result[i++] = this.maxMeasurement;

      for (int j = 0; j < this.quatizedMeasurements.length; j++) {
        result[i++] = this.quatizedMeasurements[j];
      }
      return result;
    }
  }

  @Override
  public MeasurementsInfo getInfo() {
    return info;
  }

  @Override
  @SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
  public MeasurementAccumulator aggregate(final MeasurementAccumulator mSource) {

    if (mSource instanceof QuantizedAccumulator) {
      QuantizedAccumulator other = (QuantizedAccumulator) mSource;
      long[] quantizedM;
      long minMeasurementM;
      long maxMeasurementM;
      long measurementCountM;
      long measurementTotalM;
      synchronized (this) {
        quantizedM = quatizedMeasurements.clone();
        minMeasurementM = this.minMeasurement;
        maxMeasurementM = this.maxMeasurement;
        measurementCountM = this.measurementCount;
        measurementTotalM = this.measurementTotal;
      }
      QuantizedAccumulator otherClone = other.createClone();
      final long[] lQuatizedMeas = otherClone.getQuatizedMeasurements();
      for (int i = 0; i < quantizedM.length; i++) {

        quantizedM[i] += lQuatizedMeas[i];
      }
      return new QuantizedAccumulator(info, factor, lowerMagnitude, higherMagnitude,
              Math.min(minMeasurementM, otherClone.getMinMeasurement()),
              Math.max(maxMeasurementM, otherClone.getMaxMeasurement()),
              measurementCountM + otherClone.getMeasurementCount(),
              measurementTotalM + otherClone.getMeasurementTotal(),
              quantasPerMagnitude, bucketLimits, quantizedM);
    } else {
      throw new IllegalArgumentException("Cannot aggregate " + this + " with " + mSource);
    }

  }

  @Override
  public synchronized QuantizedAccumulator createClone() {
    return new QuantizedAccumulator(info, factor, lowerMagnitude, higherMagnitude,
            minMeasurement, maxMeasurement, measurementCount, measurementTotal,
            quantasPerMagnitude, bucketLimits, quatizedMeasurements.clone());
  }

  @Override
  public synchronized QuantizedAccumulator reset() {
    if (measurementCount == 0) {
      return null;
    } else {
      QuantizedAccumulator result = createClone();
      this.minMeasurement = Long.MAX_VALUE;
      this.maxMeasurement = Long.MIN_VALUE;
      this.measurementCount = 0;
      this.measurementTotal = 0;
      Arrays.fill(this.quatizedMeasurements, 0L);
      return result;
    }
  }

  @Override
  public synchronized String toString() {
    return "QuantizedRecorder{" + "info=" + info + ", minMeasurement=" + minMeasurement + ", maxMeasurement="
            + maxMeasurement + ", measurementCount=" + measurementCount + ", measurementTotal="
            + measurementTotal + ", quantasPerMagnitude=" + quantasPerMagnitude + ", magnitudes="
            + Arrays.toString(bucketLimits) + ", quatizedMeasurements="
            + Arrays.toString(quatizedMeasurements) + '}';
  }

  public synchronized long getMaxMeasurement() {
    return maxMeasurement;
  }

  public synchronized long getMeasurementCount() {
    return measurementCount;
  }

  public synchronized long getMeasurementTotal() {
    return measurementTotal;
  }

  public synchronized long getMinMeasurement() {
    return minMeasurement;
  }

  public synchronized long[] getQuatizedMeasurements() {
    return quatizedMeasurements.clone();
  }

  @Override
  public synchronized MeasurementAccumulator createLike(final Object entity) {
    return new QuantizedAccumulator(entity, info.getDescription(), getUnitOfMeasurement(),
            this.factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude);
  }

  @Override
  @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
  public long[] getThenReset() {
    final QuantizedAccumulator vals = reset();
    if (vals == null) {
      return null;
    } else {
      return vals.get();
    }
  }
}
