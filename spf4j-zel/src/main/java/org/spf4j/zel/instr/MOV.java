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

import java.util.Arrays;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.AssignableValue;
import org.spf4j.zel.vm.FuncMarker;
import org.spf4j.zel.vm.EndParamMarker;
import org.spf4j.zel.vm.Program;
import org.spf4j.zel.vm.SuspendedException;

public final class MOV extends Instruction {
    
    private static final long serialVersionUID = -7101682855885757988L;
    
    private MOV() {
    }
    
    @Override
    public void execute(final ExecutionContext context) throws SuspendedException {
        Object what = context.pop();
        Object to = context.pop();
        if (to instanceof AssignableValue) {
            AssignableValue assignTo = (AssignableValue) to;
            assignTo.assign(what);
            while (!context.isStackEmpty()) {
                // clear all values from stack.
                // only last assigned value should be on the stack.
                context.pop();
            }
            context.push(what);
        } else if (to == FuncMarker.INSTANCE) {
            Object [] params = context.popSyncStackValsUntil(EndParamMarker.INSTANCE);
            context.resultCache.putPermanentResult((Program) ((AssignableValue) context.pop()).get(),
                    Arrays.asList(params), what);
        }
        context.ip++;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new MOV();
}
