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

import java.util.Collection;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public interface ObjectBorower<T> extends Scanable<T>
{
    
    public Object REQUEST_MADE= new Object(); 
    
    /**
     * Non Blocking method.
     * @return null if request was not made, the object of available, 
     * or REQUEST_MADE if request could be made to return Object.
     */
    
    @Nullable
    Object requestReturnObject();
    
    /**
     * Non Blocking method.
     * @return null or the object if available.
     * @throws InterruptedException 
     */
    @Nullable
    T returnObjectIfAvailable();
    
    @Nullable
    Collection<T> returnObjectsIfNotNeeded();
    
}
