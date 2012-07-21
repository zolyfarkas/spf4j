/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf.impl;

import com.google.common.math.IntMath;
import com.zoltran.perf.EntityMeasurements;
import com.zoltran.perf.MeasurementProcessor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;

/**
 *
 * @author zoly
 */
@ThreadSafe
public class QuantizedRecorder implements MeasurementProcessor, Cloneable {

    private final Object measuredEntity;
    private final String unitOfMeasurement;
    private long minMeasurement;
    private long maxMeasurement;
    private long measurementCount;
    private long measurementTotal;
    private final int quantasPerMagnitude;
    private final long[] magnitudes;
    /**
     * (long.min - f ^ l), (f ^ l - f ^ (l+1)), ... (f ^(L-1) - f ^ L) (f^L -
     * long.max)
     *
     * f = 10 m = 0 M = 5
     *
     */
    private final long[] quatizedMeasurements;

    public QuantizedRecorder(Object measuredEntity, String unitOfMeasurement, int factor, int lowerMagnitude,
            int higherMagnitude, int quantasPerMagnitude) {
        assert (quantasPerMagnitude <= factor);
        assert (lowerMagnitude < higherMagnitude);
        assert (quantasPerMagnitude > 0);
        minMeasurement = Long.MAX_VALUE;
        maxMeasurement = Long.MIN_VALUE;
        measurementCount = 0;
        measurementTotal = 0;
        this.measuredEntity = measuredEntity;
        this.quatizedMeasurements = new long[(higherMagnitude - lowerMagnitude) * quantasPerMagnitude + 2];
        this.unitOfMeasurement = unitOfMeasurement;
        this.quantasPerMagnitude = quantasPerMagnitude;
        magnitudes = new long[higherMagnitude - lowerMagnitude + 1];

        int idx = 0;
        if (lowerMagnitude < 0) {
            int toMagnitude = Math.min(-1, higherMagnitude);
            int toValue = -IntMath.pow(factor, -toMagnitude);
            int j = idx = toMagnitude - lowerMagnitude;
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
    }


    private QuantizedRecorder(Object measuredEntity, String unitOfMeasurement, long minMeasurement, long maxMeasurement, long measurementCount, long measurementTotal, int quantasPerMagnitude, long[] magnitudes, long[] quatizedMeasurements) {
        this.measuredEntity = measuredEntity;
        this.unitOfMeasurement = unitOfMeasurement;
        this.minMeasurement = minMeasurement;
        this.maxMeasurement = maxMeasurement;
        this.measurementCount = measurementCount;
        this.measurementTotal = measurementTotal;
        this.quantasPerMagnitude = quantasPerMagnitude;
        this.magnitudes = magnitudes;
        this.quatizedMeasurements = quatizedMeasurements;
    }
    
    
    

    public synchronized void record(long measurement) {
        measurementCount++;
        measurementTotal += measurement;  // TODO: check for overflow
        if (measurement < minMeasurement) {
            minMeasurement = measurement;
        }
        if (measurement > maxMeasurement) {
            maxMeasurement = measurement;
        }
        long m0 = magnitudes[0];
        if (m0 > measurement) {
            quatizedMeasurements[0]++;
        } else {
            long prevMag = m0;
            int i = 1;
            for (; i < magnitudes.length; i++) {
                long mag = magnitudes[i];
                if (mag > measurement) {
                    int qidx = (i - 1) * quantasPerMagnitude + (int) (quantasPerMagnitude
                            * (((double) (measurement - prevMag)) / (mag - prevMag))) + 1;
                    quatizedMeasurements[qidx]++;
                    break;
                }
                prevMag = mag;
            }
            if (i == magnitudes.length) {
                quatizedMeasurements[quatizedMeasurements.length - 1]++;
            }


        }

    }

    public synchronized Map<String, Number> getMeasurements(boolean reset) {
        Map<String, Number> result = new HashMap<String, Number>();
        result.put("min", this.minMeasurement);
        result.put("max", this.maxMeasurement);
        result.put("total", this.measurementTotal);
        result.put("count", this.measurementCount);
        result.put("QNI_" + this.magnitudes[0] , this.quatizedMeasurements[0]);
        if (magnitudes.length > 0) {
            int k = 1;
            long prevVal = magnitudes[0];
            for (int i = 1; i < magnitudes.length; i++) {
                long magVal = magnitudes[i];
                long intSize = magVal - prevVal;
                for (int j = 0; j < quantasPerMagnitude; j++) {
                    result.put("Q" + (prevVal + intSize * j / quantasPerMagnitude)
                            + "_" + (prevVal + intSize * (j + 1) / quantasPerMagnitude) , this.quatizedMeasurements[k++]);
                }
                prevVal = magVal;
            }
            result.put("Q" + prevVal
                    + "_PI", this.quatizedMeasurements[k]);
        }
        if (reset)
            reset();
        return result;
    }
    
    /**
     * this class ordering is based on start Interval ordering
     */ 
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS")
    public static class Quanta implements Comparable<Quanta> {
        private final long intervalStart;
        private final long intervalEnd;

        public Quanta(long intervalStart, long intervalEnd) {
            this.intervalStart = intervalStart;
            this.intervalEnd = intervalEnd;
        }
        
        public Quanta(String stringVariant) {
            int undLocation = stringVariant.indexOf('_');
            if (undLocation < 0)
                throw new IllegalArgumentException("Invalid Quanta DataSource " + stringVariant);
            String startStr = stringVariant.substring(1, undLocation);
            String endStr = stringVariant.substring(undLocation+1);
            if (startStr.equals("NI"))
                this.intervalStart = Long.MIN_VALUE;
            else 
                this.intervalStart = Long.valueOf(startStr);
            if (endStr.equals("PI"))
                this.intervalEnd = Long.MAX_VALUE;
            else 
                this.intervalEnd = Long.valueOf(endStr);
        }

        public long getIntervalEnd() {
            return intervalEnd;
        }

        public long getIntervalStart() {
            return intervalStart;
        }

        
        public long getClosestToZero() {
            return (intervalStart < 0) ? intervalEnd : intervalStart;
        }

        
        
        @Override
        public String toString() {
            return "Q" + 
                    ((intervalStart == Long.MIN_VALUE) ? "NI" : intervalStart) +
                    "_" +
                    ((intervalEnd == Long.MAX_VALUE) ? "PI" : intervalEnd);
        }

        public int compareTo(Quanta o) {
            if (this.intervalStart < o.intervalStart)
                return -1;
            else if (this.intervalStart > o.intervalStart)
                return 1;
            else 
                return 0;              
        }
        
        
    }
 

    public synchronized EntityMeasurements aggregate(EntityMeasurements mSource) {
       
        QuantizedRecorder other = (QuantizedRecorder) mSource;
        synchronized (other) {
            long [] quantizedM = quatizedMeasurements.clone();
            for (int i=0 ; i< quantizedM.length; i++ )
                quantizedM[i] += other.quatizedMeasurements[i];

            return new QuantizedRecorder(measuredEntity, unitOfMeasurement, 
                    Math.min(this.minMeasurement, other.minMeasurement), 
                            Math.max(this.maxMeasurement, other.maxMeasurement),
                            this.measurementCount + other.measurementCount,
                            this.measurementTotal + other.measurementTotal, 
                    quantasPerMagnitude, magnitudes, quantizedM);
        }
                
        
    }

    @Override
    public Object getMeasuredEntity() {
        return measuredEntity;
    }

    @Override
    public synchronized MeasurementProcessor createClone(boolean reset) {
        QuantizedRecorder result =  new QuantizedRecorder(measuredEntity, unitOfMeasurement, 
                minMeasurement, maxMeasurement, measurementCount, measurementTotal, 
                quantasPerMagnitude, magnitudes, quatizedMeasurements.clone());
        if (reset) {
            reset();          
        }
        return result;
    }
    
    private void reset() {
            this.minMeasurement = Long.MAX_VALUE;
            this.maxMeasurement = Long.MIN_VALUE;
            this.measurementCount = 0;
            this.measurementTotal = 0;
            Arrays.fill(this.quatizedMeasurements, 0L);     
    }

    @Override
    public String getUnitOfMeasurement() {
        return unitOfMeasurement;
    }

    @Override
    public String toString() {
        return "QuantizedRecorder{" + "measuredEntity=" + measuredEntity + ", unitOfMeasurement="
                + unitOfMeasurement + ", minMeasurement=" + minMeasurement + ", maxMeasurement="
                + maxMeasurement + ", measurementCount=" + measurementCount + ", measurementTotal="
                + measurementTotal + ", quantasPerMagnitude=" + quantasPerMagnitude + ", magnitudes="
                + Arrays.toString(magnitudes) + ", quatizedMeasurements="
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

    public synchronized EntityMeasurements createLike(Object entity) {
        QuantizedRecorder result =  new QuantizedRecorder(entity, unitOfMeasurement, 
                minMeasurement, maxMeasurement, measurementCount, measurementTotal, 
                quantasPerMagnitude, magnitudes, quatizedMeasurements.clone());
        return result;
    }
}
