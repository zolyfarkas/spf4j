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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Method;
import org.spf4j.zel.vm.Program;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.VMExecutor;
import org.spf4j.zel.vm.VMFuture;
import org.spf4j.zel.vm.ZExecutionException;


public final class CALLA extends Instruction {

    private static final long serialVersionUID = 759722625722456554L;

    private CALLA() {
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public int execute(final ExecutionContext context)
            throws ZExecutionException, InterruptedException, SuspendedException {
        Integer nrParams = (Integer) context.pop();
        final Object function = context.peekFromTop(nrParams);
        if (function instanceof Program) {
            final Program p = (Program) function;
            final ExecutionContext nctx;
            Object obj;
            Object[] parameters;
            switch (p.getType()) {
                case DETERMINISTIC:
                    parameters = CALL.getParamsSync(context, nrParams);
                    nctx = context.getSubProgramContext(p, parameters);
                    obj = context.resultCache.getResult(p, Arrays.asList(parameters), new AsyncCallable(nctx));

                    break;
                case NONDETERMINISTIC:
                    parameters = CALL.getParams(context, nrParams);
                    nctx = context.getSubProgramContext(p, parameters);
                    obj = Program.executeAsync(nctx);
                    break;
                default:
                    throw new UnsupportedOperationException(p.getType().toString());
            }
            context.push(obj);
        } else if (function instanceof Method) {
            final Object[] parameters = CALL.getParamsSync(context, nrParams);
            Future<Object> obj = context.execService.submit(new VMExecutor.Suspendable<Object>() {

                @Override
                @edu.umd.cs.findbugs.annotations.SuppressWarnings(
                "EXS_EXCEPTION_SOFTENING_HAS_CHECKED")
                public Object call() {
                    try {
                        return ((Method) function).invoke(context, parameters);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public List<VMFuture<Object>> getSuspendedAt() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
            context.push(obj);

        } else {
            throw new ZExecutionException("cannot invoke " + function);
        }
        return 1;
    }

    public static final Instruction INSTANCE = new CALLA();

    @Override
    public Object[] getParameters() {
         return org.spf4j.base.Arrays.EMPTY_OBJ_ARRAY;
   }
}
