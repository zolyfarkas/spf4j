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
import java.util.concurrent.ExecutionException;
import org.spf4j.zel.vm.AssignableValue;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Method;
import org.spf4j.zel.vm.Program;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;

public final class CALLREF extends Instruction {

    private static final long serialVersionUID = 1L;

    private final int nrParameters;

    public CALLREF(final int nrParameters) {
        this.nrParameters = nrParameters;
    }

    @Override
//    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public int execute(final ExecutionContext context)
            throws ExecutionException, InterruptedException, SuspendedException {
        final Object[] parameters;
        try {
            parameters = context.popSyncStackVals(nrParameters);
        } catch (SuspendedException e) {
            throw e;
        }
        final Object function = ((AssignableValue) context.pop()).get();

        context.push(new FunctionDeref(context, function, parameters));

        return 1;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {nrParameters};
    }

    private static final class FunctionDeref implements AssignableValue {

        private final ExecutionContext context;
        private final Object function;
        private final Object[] parameters;

        FunctionDeref(final ExecutionContext context, final Object function, final Object[] parameters) {
            this.context = context;
            this.function = function;
            this.parameters = parameters;
        }

        @Override
        public void assign(final Object object) {
            context.resultCache.putPermanentResult((Program) function,
                    Arrays.asList(parameters), object);

        }

        @Override
        public Object get() throws ExecutionException, InterruptedException {

            if (function instanceof Program) {
                final Program p = (Program) function;
                final ExecutionContext nctx;
                Object obj;
                switch (p.getType()) {
                    case DETERMINISTIC:
                        nctx = context.getSyncSubProgramContext(p, parameters);
                        obj = context.resultCache.getResult(p, Arrays.asList(parameters), new SyncCallable(nctx));

                        break;
                    case NONDETERMINISTIC:
                        nctx = context.getSyncSubProgramContext(p, parameters);
                        obj = Program.executeSync(nctx);
                        break;
                    default:
                        throw new UnsupportedOperationException(p.getType().toString());
                }
                return obj;
            } else if (function instanceof Method) {
                try {
                    return ((Method) function).invoke(context, parameters);
                } catch (Exception ex) {
                    throw new ZExecutionException("cannot invoke " + function, ex);
                }
            } else {
                throw new ZExecutionException("cannot invoke " + function);
            }

        }
    }
}
