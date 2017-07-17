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

import java.util.concurrent.ExecutionException;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.AssignableValue;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;

public final class MOVN extends Instruction {

    private static final long serialVersionUID = -7101682855885757988L;

    private final int nr;

    public MOVN(final int nr) {
        this.nr = nr;
    }

    @Override
    public int execute(final ExecutionContext context)
            throws SuspendedException, ExecutionException, InterruptedException {
        Object[] what = (Object[]) context.popSyncStackVal();
        Object[] tos = context.popStackVals(what.length);
        int i = 0;
        for (Object to : tos) {
            if (to instanceof AssignableValue) {
                AssignableValue assignTo = (AssignableValue) to;
                assignTo.assign(what[i++]);
                context.push(what);
            } else {
                throw new ZExecutionException("Lvalue expected insted of " + to);
            }
        }
        return 1;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {nr};
    }
}
