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
import com.zoltran.pool.ObjectPool;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author zoly
 */


public class ObjectHolder<T> implements ObjectPool<T>
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
    
    @Override
    public T borrowObject() throws ObjectCreationException
    {
        if (borrowed) {
           throw new IllegalStateException("Object is already borrowed"); 
        }
        if (obj == null) {
            obj = factory.create();
        } 
        borrowed = true;
        return obj;
    }

    @Override
    public void returnObject(T object)
    {
        if (!borrowed || object != obj) {
            throw new IllegalStateException("Cannot return something that was "
                    + "not borrowed from here " + object);
        }
        borrowed = false;
    }

    @Override
    public void returnObject(T object, Exception e)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void dispose() throws TimeoutException, InterruptedException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void scan(ScanHandler<T> handler)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
