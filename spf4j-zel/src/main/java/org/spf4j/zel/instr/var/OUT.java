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

import java.util.List;
import java.util.ListIterator;
import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.vm.EndParamMarker;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Method;
import org.spf4j.zel.vm.VMExecutor;
import org.spf4j.zel.vm.ZExecutionException;

public final class OUT extends Instruction implements Method {

    private static final long serialVersionUID = -1902851538294062563L;

    private OUT() {
    }

    @Override
    public void execute(final ExecutionContext context)
            throws ZExecutionException, VMExecutor.SuspendedException {
        List<Object> params = context.popSyncStackValsUntil(EndParamMarker.INSTANCE);
        for (Object obj : params) {
            context.out.print(obj);
        }
        context.ip++;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new OUT();

    @Override
    public Object invokeInverseParamOrder(final List<Object> parameters) throws Exception {
        ListIterator<Object> listIterator = parameters.listIterator(parameters.size());

        while (listIterator.hasPrevious()) {
            System.out.println(listIterator.previous());
        }
        return null;
    }
}
