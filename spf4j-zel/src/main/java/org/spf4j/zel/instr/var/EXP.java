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

import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.operators.Operator;
import org.spf4j.zel.operators.Operators;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.VMExecutor;



/**
 *
 * @author zoly
 */
public final class EXP extends Instruction {

    private static final long serialVersionUID = -104479947741779060L;

    private EXP() {
    }

    @Override
    public final void execute(final ExecutionContext context) throws VMExecutor.SuspendedException {
        final Number val = ((Number) context.popSyncStackVal()).doubleValue();
        context.pop();
        context.push(Operators.apply(Operator.Enum.Pow, Math.E, val));
        context.ip++;
    }

    /**
     * instance
     */
    public static final Instruction INSTANCE = new EXP();
}
