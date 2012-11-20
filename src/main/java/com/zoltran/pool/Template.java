
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


package com.zoltran.pool;

import com.zoltran.base.Callables;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class Template<T>
{

    private final ObjectPool<T> pool;
    private final int nrImmediateRetries;
    private final int nrTotalRetries;
    private final int retryWaitMillis;

    public Template(ObjectPool<T> pool, int nrImmediateRetries, int nrTotalRetries, int retryWaitMillis)
    {
        this.pool = pool;
        this.nrImmediateRetries = nrImmediateRetries;
        this.nrTotalRetries = nrTotalRetries;
        this.retryWaitMillis = retryWaitMillis;
    }
    


    public void doOnPooledObject(final ObjectPool.Hook<T> handler)
            throws ObjectCreationException, InterruptedException, TimeoutException
    {
        Callables.executeWithRetry(new Callable<Void>() { 
            @Override
            public Void call() throws Exception
            {
                doOnPooledObject(handler, pool);
                return null;
            }
        }, nrImmediateRetries, nrTotalRetries, retryWaitMillis);
        
    }
    
    
    public static <T> void doOnPooledObject(ObjectPool.Hook<T> handler, ObjectPool<T> pool)
            throws ObjectCreationException, InterruptedException, TimeoutException
    {
        T object = pool.borrowObject();
        try {
            handler.handle(object);
            pool.returnObject(object, null);
        } catch (RuntimeException e) {
            pool.returnObject(object, e);
            throw e;
        }
    }
}

