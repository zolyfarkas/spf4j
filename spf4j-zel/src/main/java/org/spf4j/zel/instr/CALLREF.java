/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public int execute(final ExecutionContext context)
            throws ExecutionException, InterruptedException, SuspendedException {
        final Object[] parameters = context.popSyncStackVals(nrParameters);
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
        public void assign(final Object object) throws ExecutionException {
          if (function instanceof Program && ((Program) function).getType() == Program.Type.DETERMINISTIC) {
            context.getResultCache().putPermanentResult((Program) function,
                      Arrays.asList(parameters), object);
          } else {
            throw new ZExecutionException("Function " + function  + " must be deterministic to memorize value ");
          }
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
                        obj = context.getResultCache().getResult(p, Arrays.asList(parameters),
                                nctx::call);

                        break;
                    case NONDETERMINISTIC:
                        nctx = context.getSyncSubProgramContext(p, parameters);
                        try {
                          obj = nctx.call();
                        } catch (SuspendedException ex) {
                          throw new ZExecutionException("Suspension not supported for " + p, ex);
                        }
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
