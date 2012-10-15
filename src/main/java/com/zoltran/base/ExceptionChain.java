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

import javax.annotation.ParametersAreNonnullByDefault;


import com.google.common.base.Throwables;
import java.lang.reflect.Field;

/**
 * Chainable exception class;
 * @author zoly
 */
@ParametersAreNonnullByDefault
public class ExceptionChain
{
    private static final Field field;
    static {
        try {
            field = Throwable.class.getField("cause");
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
        field.setAccessible(true);
    }
    
    public static Throwable chain(Throwable t, Throwable cause) {
        Throwable rc = Throwables.getRootCause(t);
        try {
            field.set(rc, cause);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        return t;
    }
}
