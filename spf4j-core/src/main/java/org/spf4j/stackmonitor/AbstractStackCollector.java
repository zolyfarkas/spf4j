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

    
    @Override
    public final SampleNode applyOnSamples(final Function<SampleNode, SampleNode> predicate) {
        synchronized (sampleSync) {
            SampleNode result = samples;
            samples = predicate.apply(samples);
            return result;
        }
    }


    @Override
    public final SampleNode clear() {
        synchronized (sampleSync) {
            SampleNode result = samples;
            samples = null;
            return result;
        }
    }
    
    @Override
    public final void addSample(final StackTraceElement[] stackTrace) {
        synchronized (sampleSync) {
            if (samples == null) {
                samples = SampleNode.createSampleNode(stackTrace);
            } else {
                SampleNode.addToSampleNode(samples, stackTrace);
            }
        }
    }

    @Override
    public final String toString() {
        synchronized (sampleSync) {
            return "AbstractStackCollector{" + "samples=" + samples  + '}';
        }
    }

    public final int getNrNodes() {
        synchronized (sampleSync) {
            if (samples == null) {
                return 0;
            } else {
                return samples.getNrNodes();
            }
        }
    }

    
}
