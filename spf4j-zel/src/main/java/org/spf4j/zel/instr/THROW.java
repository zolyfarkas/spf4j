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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.base.Arrays;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.VMASyncFuture;
import org.spf4j.zel.vm.ZExecutionException;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public final class THROW extends Instruction {

    private static final long serialVersionUID = 1L;

    private THROW() {
    }

    @Override
    public int execute(final ExecutionContext context)
            throws ZExecutionException {
        Object param = context.popStackVal();
        throw new ZExecutionException(param);
    }

    public static final Instruction INSTANCE = new THROW();

    @Override
    public Object[] getParameters() {
        return Arrays.EMPTY_OBJ_ARRAY;
    }

    private static final  class RunnableImpl implements Runnable {

        private final ExecutionContext context;
        private final VMASyncFuture<Object> future;

        RunnableImpl(final ExecutionContext context, final VMASyncFuture<Object> future) {
            this.context = context;
            this.future = future;
        }

        @Override
        public void run() {
            Object stuff;
            do {
                stuff = context.execService.resumeSuspendables(future);
            } while (stuff == null);
        }
    }

}
