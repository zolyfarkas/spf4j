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
package org.spf4j.zel.instr.var;

import java.util.Objects;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Method;



public final class DECODE implements Method {

    private static final long serialVersionUID = -5424036740352433092L;

    private DECODE() {
    }

    public static final Method INSTANCE = new DECODE();

    @Override
    public Object invoke(final ExecutionContext context, final Object[] parameters) {
        Object expr = parameters[0];
        int i = 1;
        final int lm1 = parameters.length - 1;
        while (i < lm1) {
            if (Objects.equals(expr, parameters[i])) {
                return parameters[i + 1];
            }
            i += 2;
        }
        if (i == lm1) {
            return parameters[i];
        } else {
            return null;
        }
    }
}
