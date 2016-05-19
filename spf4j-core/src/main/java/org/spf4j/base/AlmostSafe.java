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

package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
//CHECKSTYLE:OFF
import sun.misc.Unsafe;
//CHECKSTYLE:ON

/**
 * Expose
 * @author zoly
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
public final class AlmostSafe {

    private AlmostSafe() { }

    public static final Unsafe USF;

    static {

        USF = AccessController.doPrivileged(new PrivilegedAction<Unsafe>() {
            @Override
            public Unsafe run() {
                try {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
                } catch (IllegalArgumentException | IllegalAccessException
                        | NoSuchFieldException | SecurityException ex) {
                    throw new ExceptionInInitializerError(ex);
                }
            }
        });
    }



}
