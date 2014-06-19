
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
package org.spf4j.annotations;

import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.impl.RecorderFactory;
import org.spf4j.perf.impl.NopMeasurementRecorderSource;

/**
 * I am simulating a lazy enum.
 * @author zoly
 */

public abstract class RecorderSourceInstance {

    private RecorderSourceInstance() {
    }

    public static final class RsNop extends RecorderSourceInstance {

        public static final MeasurementRecorderSource INSTANCE = NopMeasurementRecorderSource.INSTANCE;
    }

    public static final class Rs5m extends RecorderSourceInstance {

        public static final MeasurementRecorderSource INSTANCE =
                RecorderFactory.createScalableQuantizedRecorderSource(Rs5m.class,
                "ms", 300000, 10, 0, 6, 10);
    }

    public static final class Rs1m extends RecorderSourceInstance {

        public static final MeasurementRecorderSource INSTANCE =
                RecorderFactory.createScalableQuantizedRecorderSource(Rs1m.class,
                "ms", 60000, 10, 0, 6, 10);
    }

    public static final class Rs15m extends RecorderSourceInstance {

        public static final MeasurementRecorderSource INSTANCE =
                RecorderFactory.createScalableQuantizedRecorderSource(Rs15m.class,
                "ms", 900000, 10, 0, 6, 10);
    }

    public static final class Rs1h extends RecorderSourceInstance {

        public static final MeasurementRecorderSource INSTANCE =
                RecorderFactory.createScalableQuantizedRecorderSource(Rs1h.class,
                "ms", 3600000, 10, 0, 6, 10);
    }
}
