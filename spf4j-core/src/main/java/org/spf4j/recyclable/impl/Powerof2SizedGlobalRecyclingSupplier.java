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
package org.spf4j.recyclable.impl;

import java.lang.ref.Reference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.spf4j.base.IntMath;
import org.spf4j.base.ReferenceType;
import org.spf4j.recyclable.SizedRecyclingSupplier;

/**
 * recycling supplier that allows you to recycle objects.
 * Recycling objects is dangerous business, care should be used.
 * @author zoly
 */
public final class Powerof2SizedGlobalRecyclingSupplier<T> implements SizedRecyclingSupplier<T> {

    private final SizedRecyclingSupplier.Factory<T> factory;

    private final ReferenceType refType;

    private final BlockingQueue<Reference<T>>[] objects;

    public Powerof2SizedGlobalRecyclingSupplier(final Factory<T> factory, final ReferenceType refType) {
        this.factory = factory;
        this.refType = refType;
        objects = new BlockingQueue[28];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new LinkedBlockingQueue<>();
        }
    }

    @Override
    public T get(final int size) {
        int idx = IntMath.closestPowerOf2(size);
        BlockingQueue<Reference<T>> refs = objects[idx];
        Reference<T> ref;
        do {
            ref = refs.poll();
            if (ref == null) {
                int actualSize = 1 << idx;
                return factory.create(actualSize);
            } else {
                T result = ref.get();
                if (result != null) {
                    return result;
                }
            }
        } while (true);
    }

    @Override
    public void recycle(final T object) {
        int size = factory.size(object);
        int idx = IntMath.closestPowerOf2(size);
        BlockingQueue<Reference<T>> refs = objects[idx];
        refs.add(refType.create(object));
    }

}
