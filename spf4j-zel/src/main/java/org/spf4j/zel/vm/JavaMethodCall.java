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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import org.spf4j.base.Reflections;

/**
 *
 * @author zoly
 */
// Will have to see if the exception handling can be improved here later
@SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
public final class JavaMethodCall implements Method {

    private final String name;
    private final Class<?> objectClass;
    private final Object object;

    public JavaMethodCall(final Object object, final String methodName) {
        this.name = methodName;
        if (object instanceof Class) {
            this.objectClass = (Class<?>) object;
            this.object = null;
        } else {
            this.objectClass = object.getClass();
            this.object = object;
        }
    }

    private static final Class<?>[] EMPTY_CL_ARR = new Class<?>[0];

    @Override
    public Object invoke(final ExecutionContext context, final Object[] parameters) {
        try {
            int np = parameters.length;
            if (np > 0) {
                Class<?>[] classes = new Class<?>[np];
                for (int i = 0; i < np; i++) {
                    classes[i] = parameters[i].getClass();
                }
                java.lang.reflect.Method m = Reflections.getCompatibleMethodCached(objectClass, name, classes);
                Class<?>[] actTypes = m.getParameterTypes();
                Class<?> lastParamClass = actTypes[actTypes.length - 1];
                if (Reflections.canAssign(lastParamClass, classes[classes.length - 1])) {
                    return m.invoke(object, parameters);
                } else if (lastParamClass.isArray()) {
                    int lidx = actTypes.length - 1;
                    int l = np - lidx;
                    Object array = Array.newInstance(lastParamClass.getComponentType(), l);
                    for (int k = 0; k < l; k++) {
                        Array.set(array, k, parameters[lidx + k]);
                    }
                    Object[] newParams = new Object[actTypes.length];
                    System.arraycopy(parameters, 0, newParams, 0, lidx);
                    newParams[lidx] = array;
                    return m.invoke(object, newParams);
                } else {
                    throw new IllegalStateException();
                }
            } else {
                return Reflections.getCompatibleMethodCached(objectClass, name, EMPTY_CL_ARR)
                        .invoke(object);
            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String toString() {
        return "JavaMethodCall{" + "name=" + name + ", objectClass=" + objectClass + ", object=" + object + '}';
    }

}
