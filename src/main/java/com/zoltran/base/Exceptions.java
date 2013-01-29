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
package com.zoltran.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Chainable exception class;
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public class Exceptions {


    public static <T extends Throwable> T chain(T t, Throwable newRootCause) {
        Throwable cause = t.getCause();
        if (cause != null) {
            cause = chain(cause, newRootCause);
        } else {
            cause = newRootCause;
        }
        Class<? extends Throwable> clasz = t.getClass();
        T result = null;
        Constructor<T>[] constructors = (Constructor<T>[]) clasz.getDeclaredConstructors();
        Arrays.sort(constructors, new Comparator<Constructor<T>>() {
            @Override
            public int compare(Constructor<T> o1, Constructor<T> o2) {
                return o2.getParameterTypes().length - o1.getParameterTypes().length;
            }
        });

        List<Constructor<T>> unsupportedConstructors = null;
        for (Constructor<T> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            try {
                if (parameterTypes.length == 2 && parameterTypes[0].equals(String.class)
                        && parameterTypes[1].equals(Throwable.class)) {
                    result = constructor.newInstance(t.getMessage(), cause);
                    break;
                } else if (parameterTypes.length == 1 && parameterTypes[0].equals(String.class)) {
                    result = constructor.newInstance(t.getMessage());
                    result.initCause(cause);
                    break;
                } else if (parameterTypes.length == 0) {
                    result = constructor.newInstance();
                    result.initCause(cause);
                    break;
                } else {
                    if (unsupportedConstructors == null) {
                        unsupportedConstructors = new ArrayList<Constructor<T>>();
                    }
                    unsupportedConstructors.add(constructor);
                }

            } catch (InstantiationException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
        if (result == null) {
            throw new RuntimeException("Unable to clone exception " + t + " unsupp constructors: " + unsupportedConstructors);
        }

        result.setStackTrace (t.getStackTrace());
        return result ;
}
}
