/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.recyclable.impl;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.spf4j.base.ReferenceType;
import org.spf4j.recyclable.SizedRecyclingSupplier;

/**
 * recycling supplier that allows you to recycle objects.
 * Recycling objects is dangerous business, care should be used.
 * @author zoly
 */
public final class SizedThreadLocalRecyclingSupplier<T> implements SizedRecyclingSupplier<T> {

    private final SizedRecyclingSupplier.Factory<T> factory;

    private final ReferenceType refType;

    private final ThreadLocal<NavigableMap<Integer, Collection<Reference<T>>>> localObjects;

    public SizedThreadLocalRecyclingSupplier(final Factory<T> factory, final ReferenceType refType) {
        this.factory = factory;
        this.refType = refType;
        localObjects = new ThreadLocal<NavigableMap<Integer, Collection<Reference<T>>>>() {

            @Override
            protected  NavigableMap<Integer, Collection<Reference<T>>> initialValue() {
                return new TreeMap<>();
            }
        };
    }

    @Override
    public T get(final int size) {
        NavigableMap<Integer, Collection<Reference<T>>> available = localObjects.get();
        if (available.isEmpty()) {
            return factory.create(size);
        } else {

            Iterator<Collection<Reference<T>>> iterator =
                    available.tailMap(size).values().iterator();
            while (iterator.hasNext()) {
                Collection<Reference<T>> refs = iterator.next();
                Iterator<Reference<T>> srit = refs.iterator();
                while (srit.hasNext()) {
                    Reference<T> next = srit.next();
                    T value = next.get();
                    iterator.remove();
                    if (value != null) {
                        return value;
                    }
                }
                if (refs.isEmpty()) {
                    iterator.remove();
                }
            }
            iterator = available.headMap(size).values().iterator();
            // replace one small array
            boolean done = false;
            while (!done && iterator.hasNext()) {
                Collection<Reference<T>> refs = iterator.next();
                Iterator<Reference<T>> srit = refs.iterator();
                while (srit.hasNext()) {
                    Reference<T> next = srit.next();
                    T value = next.get();
                    iterator.remove();
                    if (value != null) {
                       done = true;
                       break;
                    }
                }
                if (refs.isEmpty()) {
                    iterator.remove();
                }
            }
            return factory.create(size);
        }
    }

    @Override
    public void recycle(final T object) {
        NavigableMap<Integer, Collection<Reference<T>>> available = localObjects.get();
        final Integer size = factory.size(object);
        Collection<Reference<T>> refs = available.get(size);
        if (refs == null) {
            refs = new ArrayList<>(2);
            available.put(size, refs);
        }
        refs.add(refType.create(object));
    }

    @Override
    public String toString() {
        return "SizedThreadLocalRecyclingSupplier{" + "factory=" + factory + ", refType=" + refType + '}';
    }

}
