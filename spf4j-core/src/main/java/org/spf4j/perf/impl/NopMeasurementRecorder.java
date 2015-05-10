
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

import org.spf4j.perf.MeasurementRecorder;

/**
 * @author zoly
 */


public final class NopMeasurementRecorder implements MeasurementRecorder {
    private NopMeasurementRecorder() { }

    public static final NopMeasurementRecorder INSTANCE = new NopMeasurementRecorder();

    @Override
    public void record(final long measurement) {
        //NOP
    }

    @Override
    public void recordAt(final long measurement, final long timestampMillis) {
        //NOP
    }

    @Override
    public void close() {
        //Default do nothing
    }

}
