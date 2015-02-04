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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import org.spf4j.base.IntMath;
import org.spf4j.base.ReferenceType;
import org.spf4j.recyclable.SizedRecyclingSupplier;

/**
 * recycling supplier that allows you to recycle objects.
 * Recycling objects is dangerous business, care should be used.
 * @author zoly
 */
public final class SizedThreadLocalRecyclingSupplier2<T> implements SizedRecyclingSupplier<T> {

    private final SizedRecyclingSupplier.Factory<T> factory;

    private final ReferenceType refType;

    private final ThreadLocal<Deque<Reference<T>> []> localObjects;

    public SizedThreadLocalRecyclingSupplier2(final Factory<T> factory, final ReferenceType refType) {
        this.factory = factory;
        this.refType = refType;
        localObjects = new ThreadLocal<Deque<Reference<T>> []>() {

            @Override
            protected  Deque<Reference<T>> [] initialValue() {
                Deque<Reference<T>> [] result =  new Deque[28];
                for (int i = 0; i < result.length; i++) {
                    result[i] = new ArrayDeque<>();
                }
                return result;
            }
        };
    }

    @Override
    public T get(final int size) {
        Deque<Reference<T>> [] available = localObjects.get();
        int idx = IntMath.closestPowerOf2(size);
        Deque<Reference<T>> refs = available[idx];
        if (refs.isEmpty()) {
            int actualSize = 1 << idx;
            return factory.create(actualSize);
        } else {
            T result;
            do {
                Reference<T> removeLast = refs.removeLast();
                result = removeLast.get();
            } while (result == null && !refs.isEmpty());
            if (result == null) {
                int actualSize = 1 << idx;
                return factory.create(actualSize);
            } else {
                return result;
            }
        }
    }

    @Override
    public void recycle(final T object) {
        int size = factory.size(object);
        int idx = IntMath.closestPowerOf2(size);
        Deque<Reference<T>> [] available = localObjects.get();
        Deque<Reference<T>> refs = available[idx];
        refs.addLast(refType.create(object));
    }

}
