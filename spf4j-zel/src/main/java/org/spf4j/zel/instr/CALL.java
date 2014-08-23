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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Method;
import org.spf4j.zel.vm.Program;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;


public final class CALL extends Instruction {

    private static final long serialVersionUID = 759722625722456554L;

    private CALL() {
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public int execute(final ExecutionContext context)
            throws ZExecutionException, InterruptedException, SuspendedException {
        Integer nrParams = (Integer) context.pop();
        Object function = context.peekFromTop(nrParams);
        if (function instanceof Program) {
            final Program p = (Program) function;
            final ExecutionContext nctx;
            Object obj;
            Object[] parameters;
            switch (p.getType()) {
                case DETERMINISTIC:
                    parameters = getParamsSync(context, nrParams);
                    nctx = context.getSubProgramContext(p, parameters);
                    obj = context.resultCache.getResult(p,  Arrays.asList(parameters), new SyncAsyncCallable(nctx));

                    break;
                case NONDETERMINISTIC:
                        parameters = getParams(context, nrParams);
                        nctx = context.getSubProgramContext(p, parameters);
                        obj = Program.executeSyncOrAsync(nctx);
                    break;
                default:
                    throw new UnsupportedOperationException(p.getType().toString());
            }
            context.push(obj);
        } else if (function instanceof Method) {
            Object[] parameters = getParamsSync(context, nrParams);
            try {
                context.push(((Method) function).invoke(context, parameters));
            } catch (IllegalAccessException ex) {
                throw new ZExecutionException("cannot invoke " + function, ex);
            } catch (InvocationTargetException ex) {
                throw new ZExecutionException("cannot invoke " + function, ex);
            } catch (Exception ex) {
                throw new ZExecutionException("cannot invoke " + function, ex);
            }
        } else {
            throw new ZExecutionException("cannot invoke " + function);
        }
        return 1;
    }

    static Object[] getParamsSync(final ExecutionContext context, final Integer nrParams)
            throws SuspendedException {
        Object [] parameters;
        try {
            parameters = context.popSyncStackVals(nrParams);
        } catch (SuspendedException e) {
            context.push(nrParams); // put back param count so tat stack is identical.
            throw e;
        }
        context.pop(); // extract function
        return parameters;
    }
    
    static Object[] getParams(final ExecutionContext context, final Integer nrParams)
            throws SuspendedException {
        Object [] parameters = context.popStackVals(nrParams);
        context.pop();
        return parameters;
    }

    public static final Instruction INSTANCE = new CALL();

    @Override
    public Object[] getParameters() {
        return org.spf4j.base.Arrays.EMPTY_OBJ_ARRAY;
    }

}
