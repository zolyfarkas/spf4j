
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
package com.zoltran.perf;

import com.zoltran.perf.impl.QuantizedRecorder;
import com.zoltran.perf.impl.ScalableMeasurementRecorder;
import com.zoltran.perf.impl.ScalableMeasurementRecorderSource;
import com.zoltran.perf.impl.rrd.RRDMeasurementDatabase;

/**
 *
 * @author zoly
 */
public final class RecorderFactory {
    
    
    static final RRDMeasurementDatabase RRD_DATABASE = 
            new RRDMeasurementDatabase(System.getProperty("rrd.perf.folder", 
            System.getProperty("java.io.tmpdir")));
    
    static {
        try
        {
            RRD_DATABASE.registerJmx();
        } catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
    
    public static MeasurementRecorder createScalableQuantizedRecorder(Object forWhat, String unitOfMeasurement, int sampleTimeMillis,
             int factor, int lowerMagnitude, 
            int higherMagnitude, int quantasPerMagnitude ) {
        return new ScalableMeasurementRecorder(new QuantizedRecorder(forWhat,
                unitOfMeasurement, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude), sampleTimeMillis, RRD_DATABASE);
    }
    
    
    public static MeasurementRecorderSource createScalableQuantizedRecorderSource(Object forWhat, String unitOfMeasurement, int sampleTimeMillis,
             int factor, int lowerMagnitude, 
            int higherMagnitude, int quantasPerMagnitude ) {
        return new ScalableMeasurementRecorderSource(new QuantizedRecorder(forWhat,
                unitOfMeasurement, factor, lowerMagnitude, higherMagnitude, quantasPerMagnitude), sampleTimeMillis, RRD_DATABASE);
    }
    
}
