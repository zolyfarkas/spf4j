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

package org.spf4j.zel.vm;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.spf4j.base.Reflections;

/**
 *
 * @author zoly
 */
public final class JavaMethodCall implements Method {

    private final String name;
    private final Class<?> objectClass;
    private final Object object;

    public JavaMethodCall(final Object object, final String methodName) {
        this.name = methodName;
        this.objectClass = object.getClass();
        this.object = object;
    }
    
    public JavaMethodCall(final Class<?> objectClass, final String methodName) {
        this.name = methodName;
        this.objectClass = objectClass;
        this.object = null;
    }
    
    @Override
    public Object invokeInverseParamOrder(final ExecutionContext context, final List<Object> parameters)
            throws IllegalAccessException, InvocationTargetException {
        int np = parameters.size();
        Class<?>[] classes = new Class<?>[np];
        Object[] params = new Object[np];
        int i = np - 1;
        for (Object obj : parameters) {
            Class<?> clasz = obj.getClass();
            classes[i] = clasz;
            params[i] = obj;
            i--;
        }
        return Reflections.getCompatibleMethodCached(objectClass, name, classes)
                .invoke(object, params);
    }

}
