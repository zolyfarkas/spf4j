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
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.JavaMethodCall;
import org.spf4j.zel.vm.SuspendedException;
import org.spf4j.zel.vm.ZExecutionException;

/**
 * @author zoly
 */
public final class DEREF extends Instruction {

    private static final long serialVersionUID = 1L;

    private DEREF() {
    }

//    public interface Deref<A, B> {
//        Object apply(A relativeTo, B ref);
//    }
//    public static final Map<Class<?>, Deref<?,?>> DEREF = new HashMap<Class<?>, Deref<?, ?>>();
//    static {
//        DEREF.put(Map.class, new Deref<Map, Object>() {
//
//            @Override
//            public Object apply(final Map relativeTo, final Object ref) {
//                return relativeTo.get(ref);
//            }
//        });
//        DEREF.put(Object [].class, new Deref<Object [], Number>() {
//
//            @Override
//            public Object apply(final Object[] relativeTo, final Number ref) {
//                return relativeTo[ref.intValue()];
//            }
//        });
//
//    }
    
    
    
    /**
     * The instruction microcode
     * @param context ExecutionContext
     */
    @Override
    public void execute(final ExecutionContext context)
            throws ZExecutionException, SuspendedException {
       
       Object [] vals = context.popSyncStackVals(2);
       Object ref = vals[1];
       Object relativeTo = vals[0];
       
       
       
       if (relativeTo instanceof Map) {
           context.push(((Map) relativeTo).get(ref));
       } else if (relativeTo instanceof Object[]) {
           context.push(((Object []) relativeTo)[((Number) ref).intValue()]);
       } else if (relativeTo instanceof int[]) {
           context.push(((int []) relativeTo)[((Number) ref).intValue()]);
       } else if (relativeTo instanceof byte[]) {
           context.push(((byte []) relativeTo)[((Number) ref).intValue()]);
       } else if (relativeTo instanceof char[]) {
           context.push(((char []) relativeTo)[((Number) ref).intValue()]);
       } else if (relativeTo instanceof long[]) {
           context.push(((long []) relativeTo)[((Number) ref).intValue()]);
       } else if (relativeTo instanceof short[]) {
           context.push(((short []) relativeTo)[((Number) ref).intValue()]);
       } else if (relativeTo instanceof List) {
           context.push(((List) relativeTo).get(((Number) ref).intValue()));
       } else {
           context.push(new JavaMethodCall(relativeTo, (String) ref));
       }
       context.ip++;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new DEREF();
}
