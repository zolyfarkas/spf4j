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


import java.util.Iterator;
import java.util.List;
import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.vm.EndParamMarker;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.VMExecutor;
import org.spf4j.zel.vm.ZExecutionException;

public final class MIN extends Instruction {

    private static final long serialVersionUID = -7763353247637263363L;

    private MIN() {
    }

    @Override
    public void execute(final ExecutionContext context)
            throws ZExecutionException, VMExecutor.SuspendedException {
        List<Object> params = context.popSyncStackValsUntil(EndParamMarker.INSTANCE);
        Iterator<Object> it = params.iterator();
        Comparable min = (Comparable) it.next();
        while (it.hasNext()) {
            Comparable obj = (Comparable) it.next();
            if (min.compareTo(obj) > 0) {
                min = obj;
            }
        }
        context.push(min);
        context.ip++;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new MIN();

}
