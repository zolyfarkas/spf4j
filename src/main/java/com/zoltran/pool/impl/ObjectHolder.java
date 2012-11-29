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
 

package com.zoltran.pool.impl;

import com.zoltran.pool.ObjectCreationException;
import com.zoltran.pool.ObjectDisposeException;
import com.zoltran.pool.ObjectPool;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * this is not a thread safe implementation.
 * 
 * @author zoly
 */

@ParametersAreNonnullByDefault
public class ObjectHolder<T> 
{

    private T obj;
    
    private boolean borrowed;
    
    private final ObjectPool.Factory<T> factory;
    
    public ObjectHolder(ObjectPool.Factory<T> factory) {
        this.factory = factory;
        borrowed = false;
    }
    
    public ObjectHolder(ObjectPool.Factory<T> factory, boolean lazy) throws ObjectCreationException {
        this(factory);
        if (!lazy) {
            obj = factory.create();
        }
    }
    
    synchronized public T borrowOrCreateObjectIfPossible() throws ObjectCreationException
    {
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
    synchronized public T borrowObjectIfAvailable()
    {
        if (borrowed || obj == null) {
           return null; 
        }
        borrowed = true;
        return obj;
    }

    synchronized public void returnObject(T object, Exception e) throws ObjectDisposeException
    {
        if (!borrowed || object != obj) {
            throw new IllegalStateException("Cannot return something that was "
                    + "not borrowed from here " + object);
        }
        borrowed = false;
        if (e != null && factory.validate(object, e) != null) {
            obj = null;
            factory.dispose(object); 
        }
    }

    synchronized public boolean disposeIfNotBorrowed() throws ObjectDisposeException
    {
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
    synchronized public String toString() {
        return "ObjectHolder{" + "obj=" + obj + ", borrowed=" + borrowed + ", factory=" + factory + '}';
    }
    
    
}
