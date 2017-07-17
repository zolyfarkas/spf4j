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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.JavaMethodCall;
import org.spf4j.zel.vm.SuspendedException;


public final class DEREF extends Instruction {

    private static final long serialVersionUID = 1L;

    public static final Instruction INSTANCE = new DEREF();

    private DEREF() {
    }

    @Override
    public int execute(final ExecutionContext context)
            throws ExecutionException, SuspendedException {
       final Object[] vals = context.tuple();
       context.popSyncStackVals(vals);
       pushDeref(vals[0], vals[1], context);
       return 1;
    }

    static void pushDeref(final Object relativeTo, final Object ref, final ExecutionContext context) {
        if (relativeTo instanceof Map) {
            context.push(((Map) relativeTo).get(ref));
        } else if (relativeTo instanceof Object[]) {
            if ("length".equals(ref)) {
                context.push(((Object[]) relativeTo).length);
            } else {
                context.push(((Object[]) relativeTo)[((Number) ref).intValue()]);
            }
        } else if (relativeTo instanceof int[]) {
            context.push(((int[]) relativeTo)[((Number) ref).intValue()]);
        } else if (relativeTo instanceof byte[]) {
            context.push(((byte[]) relativeTo)[((Number) ref).intValue()]);
        } else if (relativeTo instanceof char[]) {
            context.push(((char[]) relativeTo)[((Number) ref).intValue()]);
        } else if (relativeTo instanceof long[]) {
            context.push(((long[]) relativeTo)[((Number) ref).intValue()]);
        } else if (relativeTo instanceof short[]) {
            context.push(((short[]) relativeTo)[((Number) ref).intValue()]);
        } else if (relativeTo instanceof List) {
            context.push(((List) relativeTo).get(((Number) ref).intValue()));
        } else {
            context.push(new JavaMethodCall(relativeTo, (String) ref));
        }
    }

    @Override
    public Object[] getParameters() {
        return org.spf4j.base.Arrays.EMPTY_OBJ_ARRAY;
    }
}
