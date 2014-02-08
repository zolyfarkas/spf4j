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

import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.instr.Instruction;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;

/**
 *
 * @author zoly
 */
public final class INT extends Instruction {

    /**
     * instance
     */
    public static final Instruction INSTANCE = new INT();

    private static final long serialVersionUID = 5154431044890636019L;

    private INT() {
    }

    /**
     * experimental cast
     *
     * @param context ExecutionContext
     * @throws ZExecutionException
     */
    @Override
    public void execute(final ExecutionContext context)
            throws ZExecutionException, SuspendedException {
        Number val = (Number) context.popSyncStackVal();
        context.pop();
        if (val instanceof Integer || val instanceof Short || val instanceof Byte) {
            context.push(val.intValue());
        } else if (val instanceof Long) {
            context.push(val.longValue());
        } else if (val instanceof Double) {
            context.push(new BigDecimal(val.doubleValue()).toBigInteger());
        } else if (val instanceof BigDecimal) {
            context.push(((BigDecimal) val).toBigInteger());
        } else if (val instanceof BigInteger) {
            context.push(val);
        } else {
            throw new ZExecutionException("Unsupported argument " + val);
        }
        context.ip++;

    }
}
