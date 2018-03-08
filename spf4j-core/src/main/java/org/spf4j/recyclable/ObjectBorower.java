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

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.spf4j.base.Either;

/**
 *
 * @author zoly
 */
public interface ObjectBorower<T> extends Scanable<T> {


    enum Action { REQUEST_MADE, NONE }

    /**
     * Non Blocking method.
     *
     * @return Ether object or REQUEST_MADE if was able to notify borrower to return an object, or NONE if not.
     * Object.
     */
    @Nonnull
    Either<Action, T> tryRequestReturnObject() throws InterruptedException;

    /**
     * Non Blocking method.
     *
     * @return null or the object if available.
     * @throws InterruptedException
     */
    @Nullable
    T tryReturnObjectIfNotInUse() throws InterruptedException;


    /**
     * Return all objects that are not currently in use.
     */

    @Nonnull
    Collection<T> tryReturnObjectsIfNotInUse() throws InterruptedException;

    /**
     * This method is a cleanup method. The purpose is to recover all borrowed objects before once this borrower will
     * never use them anymore...
     *
     * @return all objects borrowed
     */
    @Nonnull
    Collection<T> tryReturnObjectsIfNotNeededAnymore() throws InterruptedException;


    /**
     * Notify borower that object has been received back from another borrower.
     * return true is indeed object was from here, false otherwise.
     * @param object
     */
    boolean nevermind(T object);


}
