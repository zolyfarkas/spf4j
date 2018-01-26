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

import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import java.util.LinkedList;
import java.util.Queue;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class ObjectHolderFactory<T> implements RecyclingSupplier.Factory<ObjectHolder<T>> {

    private final Queue<ObjectHolder<T>> objects;
    private final RecyclingSupplier.Factory<T> factory;

    public ObjectHolderFactory(final int precreateNumber, final RecyclingSupplier.Factory<T> factory)
            throws ObjectCreationException {
        objects = new LinkedList<>();
        this.factory = factory;
        for (int i = 0; i < precreateNumber; i++) {
            objects.add(new ObjectHolder<>(factory, false));
        }
    }

    @Override
    public ObjectHolder<T> create() throws ObjectCreationException {
        if (objects.isEmpty()) {
            return new ObjectHolder<>(factory);
        } else {
            return objects.remove();
        }
    }

    @Override
    public void dispose(final ObjectHolder<T> object) throws ObjectDisposeException {
        if (!object.disposeIfNotBorrowed()) {
            throw new IllegalArgumentException("Object from holder is borrowed " + object);
        }
    }

    @Override
    public boolean validate(final ObjectHolder<T> object, final Exception e) {
        return true;
    }

    @Override
    public String toString() {
        return "ObjectHolderFactory{" + "objects=" + objects + ", factory=" + factory + '}';
    }


}
