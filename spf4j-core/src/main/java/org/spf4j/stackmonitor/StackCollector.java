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

/**
 * @author zoly
 */
public interface StackCollector {

    /**
     * Apply function on the collected samples.
     * 
     * @param transform - the function to apply on samples.
     * @return - sample node before function was applied.
     */
    SampleNode applyOnSamples(Function<SampleNode, SampleNode> transform);

    SampleNode clear();

    void sample(Thread ignore);
    
    void addSample(StackTraceElement[] stackTrace);
    
}
