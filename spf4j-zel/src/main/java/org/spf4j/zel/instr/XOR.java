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

import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;



/**
 *
 * @author zoly
 */
public final class XOR extends Instruction {

    private static final long serialVersionUID = 173371262128951181L;

    private XOR() {
    }

    @Override
    public void execute(final ExecutionContext context)
            throws ZExecutionException, SuspendedException {
        Object [] vals = context.popSyncStackVals(2);
        boolean v1 = (java.lang.Boolean) vals[0];
        boolean v2 = (java.lang.Boolean) vals[1];
        context.push(Boolean.valueOf(v1 ^ v2));
        context.ip++;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new XOR();
}
