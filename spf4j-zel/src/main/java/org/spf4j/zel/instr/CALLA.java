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
import java.util.concurrent.ExecutionException;
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

    private final int nrParameters;

    public CALLA(final int nrParameters) {
        this.nrParameters = nrParameters;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public int execute(final ExecutionContext context)
            throws ExecutionException, InterruptedException, SuspendedException {
        final Object function = context.peekFromTop(nrParameters);
        if (function instanceof Program) {
            final Program p = (Program) function;
            final ExecutionContext nctx;
            Object obj;
            Object[] parameters;
            switch (p.getType()) {
                case DETERMINISTIC:
                    parameters = CALL.getParamsSync(context, nrParameters);
                    nctx = context.getSubProgramContext(p, parameters);
                    obj = context.getResultCache().getResult(p, Arrays.asList(parameters), new AsyncCallable(nctx));
                    break;
                case NONDETERMINISTIC:
                    parameters = CALL.getParams(context, nrParameters);
                    nctx = context.getSubProgramContext(p, parameters);
                    obj = nctx.executeAsync();
                    break;
                default:
                    throw new UnsupportedOperationException(p.getType().toString());
            }
            context.push(obj);
        } else if (function instanceof Method) {
            final Object[] parameters = CALL.getParamsSync(context, nrParameters);
            Future<Object> obj = context.getExecService().submit(new MethodVMExecutor(function, context, parameters));
            context.push(obj);
        } else {
            throw new ZExecutionException("cannot invoke " + function);
        }
        return 1;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {nrParameters};
    }

    private static class MethodVMExecutor implements VMExecutor.Suspendable<Object> {

        private final Object function;
        private final ExecutionContext context;
        private final Object[] parameters;

        MethodVMExecutor(final Object function, final ExecutionContext context, final Object[] parameters) {
            this.function = function;
            this.context = context;
            this.parameters = parameters;
        }

        @Override
        public Object call() {
            return ((Method) function).invoke(context, parameters);
        }

        @Override
        public List<VMFuture<Object>> getSuspendedAt() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
