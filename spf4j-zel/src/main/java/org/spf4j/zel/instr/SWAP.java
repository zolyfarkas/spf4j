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
package org.spf4j.zel.instr;

import java.util.concurrent.ExecutionException;
import org.spf4j.base.Arrays;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.AssignableValue;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;

public final class SWAP extends Instruction {

    private static final long serialVersionUID = -7101682855885757988L;

    private SWAP() {
    }

    @Override
    public int execute(final ExecutionContext context)
            throws SuspendedException, ExecutionException, InterruptedException {
        Object v1 = context.pop();
        Object v2 = context.pop();
        if (v1 instanceof AssignableValue &&  v2 instanceof AssignableValue) {
            AssignableValue a1 = (AssignableValue) v1;
            AssignableValue a2 = (AssignableValue) v2;
            Object tmp = a1.get();
            a1.assign(a2.get());
            a2.assign(tmp);
        } else {
            throw new ZExecutionException("Lvalue expected insted of " + v1 + " and " + v2);
        }
        return 1;
    }

    public static final Instruction INSTANCE = new SWAP();

    @Override
    public Object[] getParameters() {
        return Arrays.EMPTY_OBJ_ARRAY;
    }
}
