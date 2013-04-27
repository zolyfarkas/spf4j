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
package org.spf4j.pool.impl;

import org.spf4j.pool.ObjectCreationException;
import org.spf4j.pool.ObjectDisposeException;
import org.spf4j.pool.ObjectPool;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this is not a thread safe implementation.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class ObjectHolder<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectHolder.class);
    private T obj;
    private boolean borrowed;
    private final ObjectPool.Factory<T> factory;

    public ObjectHolder(final ObjectPool.Factory<T> factory) {
        this.factory = factory;
        borrowed = false;
    }

    public ObjectHolder(final ObjectPool.Factory<T> factory, final boolean lazy) throws ObjectCreationException {
        this(factory);
        if (!lazy) {
            obj = factory.create();
        }
    }

    public synchronized T borrowOrCreateObjectIfPossible() throws ObjectCreationException {
        if (borrowed) {
            return null;
        }
        if (obj == null) {
            obj = factory.create();
        }
        borrowed = true;
        return obj;
    }

    @Nullable
    public synchronized T borrowObjectIfAvailable() {
        if (borrowed || obj == null) {
            return null;
        }
        borrowed = true;
        return obj;
    }

    public synchronized void returnObject(final T object, final Exception e) throws ObjectDisposeException {
        if (!borrowed || object != obj) {
            throw new IllegalStateException("Cannot return something that was "
                    + "not borrowed from here " + object);
        }
        borrowed = false;
        if (e != null) {
            Exception ex = safeValidate(e);
            LOG.debug("Object {} returned with exception {} validation failed", obj, e, ex);
            if (ex != null) {
                obj = null;
                factory.dispose(object);
            }
        }
    }

    public synchronized void validateObjectIfNotBorrowed() throws ObjectDisposeException {
        if (!borrowed && obj != null) {
            Exception e = safeValidate(null);
            LOG.debug("Object {} validation failed", obj, e);
            if (e != null) {
                T object = obj;
                obj = null;
                factory.dispose(object);
            }
        }
    }

    public synchronized boolean disposeIfNotBorrowed() throws ObjectDisposeException {
        if (borrowed) {
            return false;
        }
        if (obj != null) {
            factory.dispose(obj);
            obj = null;
        }
        return true;
    }

    @Override
    public synchronized String toString() {
        return "ObjectHolder{" + "obj=" + obj + ", borrowed=" + borrowed + ", factory=" + factory + '}';
    }

    /**
     * Protects against poor validation implementations.
     *
     * @return
     * @throws RuntimeException
     * @throws ObjectDisposeException
     */
    private Exception safeValidate(@Nullable final Exception exc)  {
        Exception e;
        try {
            e = factory.validate(obj, exc);
        } catch (Exception ex) {
            e = ex;
        }
        return e;
    }
}
