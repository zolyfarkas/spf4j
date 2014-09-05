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
package org.spf4j.recyclable;

import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Object pool interface.
 *
 * My goal is to create a simpler and better object pool interface and implementation.
 *
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public interface RecyclingSupplier<T> extends Disposable {

    /**
     * block until a object is available and return it.
     *
     * @return
     * @throws ObjectCreationException
     * @throws ObjectBorrowException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Nonnull
    T get() throws ObjectCreationException, ObjectBorrowException,
            InterruptedException, TimeoutException;

    /**
     * return a object previously borrowed from the pool,
     * together with a optional exception in case one was encountered
     * while using the object. passing an exception will cause the object
     * to be validated and potentially retired from the pool.
     *
     * @param object
     * @param e
     */
    void recycle(T object, @Nullable Exception e);
    
    

    @ParametersAreNonnullByDefault
    public interface Factory<T> {

        /**
         * create the object.
         *
         * @return
         * @throws ObjectCreationException
         */
        T create() throws ObjectCreationException;

        /**
         * Dispose the object.
         *
         * @param object
         * @throws ObjectDisposeException
         */
        void dispose(T object) throws ObjectDisposeException;

        /**
         * Validate the object, return true if valid,
         * false of throw an Exception with validation detail otherwise.
         */
        @Nullable
        @CheckReturnValue
        boolean validate(T object, @Nullable Exception e) throws Exception;
        
        
    }
}
