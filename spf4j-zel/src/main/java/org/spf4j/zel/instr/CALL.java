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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
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
            switch (p.getType()) {
                case DETERMINISTIC:
                    nctx = context.getSubProgramContext(p, nrParameters);
                    context.pop();
                    List<Object> params = getParameters(nctx, nrParameters);
                    obj = context.getResultCache().getResult(p,  params, nctx::executeSyncOrAsync);
                    break;
                case NONDETERMINISTIC:
                        nctx = context.getSubProgramContext(p, nrParameters);
                        context.pop();
                        obj = nctx.executeSyncOrAsync();
                    break;
                default:
                    throw new UnsupportedOperationException(p.getType().toString());
            }
            context.push(obj);
        } else if (function instanceof Method) {
            Object[] parameters = context.popSyncStackVals(nrParameters);
            context.pop();
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

  public static List<Object> getParameters(final ExecutionContext nctx, final int nrParameters) {
    List<Object> params = new ArrayList<>(nrParameters);
    for (int i = 0; i < nrParameters; i++) {
      params.add(nctx.localPeek(i));
    }
    return params;
  }

    @Override
    public Object[] getParameters() {
        return new Object[] {nrParameters};
    }

}
