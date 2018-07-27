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
package org.spf4j.recyclable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Object pool interface.
 *
 * My goal is to create a simpler and better object pool interface and implementation.
 *
 *
 * @author zoly
 * @param <T> - type of recycled objects
 */
@ParametersAreNonnullByDefault
public interface RecyclingSupplier<T> extends NonValidatingRecyclingSupplier<T> {

    /**
     * return a object previously borrowed from the pool,
     * together with a optional exception in case one was encountered
     * while using the object. passing an exception will cause the object
     * to be validated and potentially retired from the pool.
     *
     * @param object - object to recycle.
     * @param e - exception encountered while handling the object. this is useful for the recycle to validate/retire
     * object
     */
    void recycle(T object, @Nullable Exception e);

    /**
     * recycle object.
     * @param object - object to recycle.
     */
    default void recycle(T object) {
      recycle(object, null);
    }


    @ParametersAreNonnullByDefault
    public interface Factory<T> {

        /**
         * create the object.
         *
         * @return - the created object.
         * @throws ObjectCreationException - cannot create object.
         */
        T create() throws ObjectCreationException;

        /**
         * Dispose the object.
         *
         * @param object - object to dispose.
         * @throws ObjectDisposeException - cannot dispose object.
         */
        void dispose(T object) throws ObjectDisposeException;

        /**
         * Validate the object, return true if valid,
         * false of throw an Exception with validation detail otherwise.
         * in case of throwing an exception the object is considered invalid.
         * @param object - object to validate.
         * @param e - exception previously encountered while handling the object.
         * @return  - true is object is still valid, false otherwise.
         * @throws java.lang.Exception - something happened during validation.
         */
        @Nullable
        @CheckReturnValue
        boolean validate(T object, @Nullable Exception e) throws Exception;


    }
}
