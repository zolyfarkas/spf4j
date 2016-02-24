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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.JavaMethodCall;
import org.spf4j.zel.vm.SuspendedException;


public final class DEREF extends Instruction {

    private static final long serialVersionUID = 1L;

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
            if (ref instanceof String && ref.equals("length")) {
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

    public static final Instruction INSTANCE = new DEREF();

    @Override
    public Object[] getParameters() {
        return org.spf4j.base.Arrays.EMPTY_OBJ_ARRAY;
    }
}
