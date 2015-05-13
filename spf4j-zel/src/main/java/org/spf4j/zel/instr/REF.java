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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.spf4j.base.Arrays;
import org.spf4j.zel.vm.AssignableValue;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.JavaMethodCall;
import org.spf4j.zel.vm.SuspendedException;

/**
 * @author zoly
 */
public final class REF extends Instruction {

    private static final long serialVersionUID = 1L;

    private REF() {
    }

    @Override
    public int execute(final ExecutionContext context)
            throws ExecutionException, SuspendedException, InterruptedException {
        final Object[] vals = context.tuple();
        context.popSyncStackVals(vals);
        final Object ref = vals[1];
        final Object relTo = vals[0];
        final Object relativeTo;
        if (relTo instanceof AssignableValue) {
            Object obj = ((AssignableValue) relTo).get();
            if (obj == null) {
                obj = new HashMap();
                ((AssignableValue) relTo).assign(obj);
            }
            relativeTo = obj;
        } else {
            relativeTo = relTo;
        }
        if (relativeTo instanceof Map) {
            context.push(new MapDeref(relativeTo, ref));

        } else if (relativeTo instanceof Object[]) {
            context.push(new ArrayDeref(relativeTo, ref));

        } else {
            context.push(new JavaMethodDeref(relativeTo, ref));
        }
        return 1;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new REF();

    @Override
    public Object[] getParameters() {
        return Arrays.EMPTY_OBJ_ARRAY;
    }

    private static final class MapDeref implements AssignableValue {

        private final Object relativeTo;
        private final Object ref;

        public MapDeref(final Object relativeTo, final Object ref) {
            this.relativeTo = relativeTo;
            this.ref = ref;
        }

        @Override
        public void assign(final Object object) {
            ((Map) relativeTo).put(ref, object);
        }

        @Override
        public Object get() {
            return ((Map) relativeTo).get(ref);
        }
    }

    private static final class ArrayDeref implements AssignableValue {

        private final Object relativeTo;
        private final Object ref;

        public ArrayDeref(final Object relativeTo, final Object ref) {
            this.relativeTo = relativeTo;
            this.ref = ref;
        }

        @Override
        public void assign(final Object object) {
            ((Object[]) relativeTo)[((Number) ref).intValue()] = object;
        }

        @Override
        public Object get() {
            return ((Object[]) relativeTo)[((Number) ref).intValue()];
        }
    }

    private static final class JavaMethodDeref implements AssignableValue {

        private final Object relativeTo;
        private final Object ref;

        public JavaMethodDeref(final Object relativeTo, final Object ref) {
            this.relativeTo = relativeTo;
            this.ref = ref;
        }

        @Override
        public void assign(final Object object) {
            throw new UnsupportedOperationException("Cannot assign " + object + " to "
                    + relativeTo + '.' + ref);
        }

        @Override
        public Object get() {
            return new JavaMethodCall(relativeTo, (String) ref);
        }
    }
}
