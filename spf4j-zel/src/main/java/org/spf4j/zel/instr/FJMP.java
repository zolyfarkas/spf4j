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
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.SuspendedException;


public final class FJMP extends Instruction {

    private static final long serialVersionUID = 584597000187469774L;

    public FJMP(final int relAddr) {
        this.relAddr = relAddr;
    }

    private final int relAddr;

    @Override
    public int execute(final ExecutionContext context)
            throws SuspendedException, ExecutionException {
        boolean cond = (java.lang.Boolean) context.popSyncStackVal();
        if (!cond) {
            return relAddr;
        } else {
            return 1;
        }
    }

    @Override
    public Object[] getParameters() {
        return new Object [] {relAddr};
    }

}
