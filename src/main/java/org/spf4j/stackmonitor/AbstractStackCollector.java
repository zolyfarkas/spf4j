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
package org.spf4j.stackmonitor;

import com.google.common.base.Function;
import javax.annotation.concurrent.GuardedBy;

/**
 *
 * @author zoly
 */
public abstract class AbstractStackCollector implements StackCollector {
 
    
    private final Object sampleSync = new Object();
    @GuardedBy(value = "sampleSync")
    private SampleNode samples;

    private int nrNodes = 0;
    
    @Override
    public final SampleNode applyOnSamples(final Function<SampleNode, SampleNode> predicate) {
        synchronized (sampleSync) {
            samples = predicate.apply(samples);
            return samples;
        }
    }


    @Override
    public final void clear() {
        synchronized (sampleSync) {
            samples = null;
            nrNodes = 0;
        }
    }
    
    public final void addSample(final StackTraceElement[] stackTrace) {
        synchronized (sampleSync) {
            if (samples == null) {
                samples = new SampleNode(stackTrace, stackTrace.length - 1);
                nrNodes += stackTrace.length +1;
            } else {
                nrNodes += samples.addSample(stackTrace, stackTrace.length - 1);
            }
        }
    }

    @Override
    public final String toString() {
        synchronized (sampleSync) {
            return "AbstractStackCollector{" + "samples=" + samples + ", nrNodes=" + nrNodes + '}';
        }
    }

    public final int getNrNodes() {
        synchronized (sampleSync) {
            return nrNodes;
        }
    }

    
}
