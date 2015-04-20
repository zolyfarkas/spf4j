
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
package org.spf4j.perf;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 *
 * @author zoly
 */

public interface EntityMeasurements {

    @Nullable
    long [] getMeasurements();

    /**
     * @return null when no measurements have been made.
     */

    @Nullable
    long [] getMeasurementsAndReset();

    @Nonnull
    EntityMeasurements  aggregate(@Nonnull EntityMeasurements mSource);

    @Nonnull
    EntityMeasurements createClone();

    /**
     * reset this entity.
     * @return a clone of the object prior to reset or null if no measurements have been made.
     */
    @Nullable
    EntityMeasurements reset();

    @Nonnull
    EntityMeasurements createLike(@Nonnull Object entity);

    @Nonnull
    EntityMeasurementsInfo getInfo();

}
