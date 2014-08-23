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
package org.spf4j.pool;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.spf4j.base.Either;

/**
 *
 * @author zoly
 */
public interface ObjectBorower<T> extends Scanable<T> {


    public enum Action { REQUEST_MADE, NONE }
    
    /**
     * Non Blocking method.
     *
     * @return null if request was not made, the object if available, or REQUEST_MADE if request could be made to return
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
    
    @Nullable
    Collection<T> tryReturnObjectsIfNotInUse() throws InterruptedException;
    
    /**
     * This method is a cleanup method. The purpose is to recover all borrowed objects before once this borrower will
     * never use them anymore...
     *
     * @return all objects borrowed
     */
    @Nullable
    Collection<T> tryReturnObjectsIfNotNeededAnymore() throws InterruptedException;
}
