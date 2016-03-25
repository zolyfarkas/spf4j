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
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Method;
import org.spf4j.zel.vm.Program;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;


public final class CALL extends Instruction {

    private static final long serialVersionUID = 759722625722456554L;

    private final int nrParameters;

    public CALL(final int nrParameters) {
        this.nrParameters = nrParameters;
    }

    @Override
    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public int execute(final ExecutionContext context)
            throws ExecutionException, InterruptedException, SuspendedException {
        Object function = context.peekFromTop(nrParameters);
        if (function instanceof Program) {
            final Program p = (Program) function;
            final ExecutionContext nctx;
            Object obj;
            Object[] parameters;
            switch (p.getType()) {
                case DETERMINISTIC:
                    parameters = getParamsSync(context, nrParameters);
                    nctx = context.getSubProgramContext(p, parameters);
                    obj = context.getResultCache().getResult(p,  Arrays.asList(parameters),
                            new SyncAsyncCallable(nctx));

                    break;
                case NONDETERMINISTIC:
                        parameters = getParams(context, nrParameters);
                        nctx = context.getSubProgramContext(p, parameters);
                        obj = nctx.executeSyncOrAsync();
                    break;
                default:
                    throw new UnsupportedOperationException(p.getType().toString());
            }
            context.push(obj);
        } else if (function instanceof Method) {
            Object[] parameters = getParamsSync(context, nrParameters);
            try {
                context.push(((Method) function).invoke(context, parameters));
            } catch (RuntimeException ex) {
                throw new ZExecutionException("cannot invoke " + function, ex);
            }
        } else {
            throw new ZExecutionException("cannot invoke " + function);
        }
        return 1;
    }

    static Object[] getParamsSync(final ExecutionContext context, final Integer nrParams)
            throws SuspendedException, ExecutionException {
        Object[] parameters;
        try {
            parameters = context.popSyncStackVals(nrParams);
        } catch (SuspendedException e) {
            throw e;
        }
        context.pop(); // extract function
        return parameters;
    }

    static Object[] getParams(final ExecutionContext context, final Integer nrParams) {
        Object[] parameters = context.popStackVals(nrParams);
        context.pop(); // pop the called method out.
        return parameters;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {nrParameters};
    }

}
