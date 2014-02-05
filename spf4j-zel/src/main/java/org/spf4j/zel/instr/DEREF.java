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

import java.util.Map;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.JavaMethodCall;
import org.spf4j.zel.vm.VMExecutor;
import org.spf4j.zel.vm.ZExecutionException;

/**
 * @author zoly
 */
public final class DEREF extends Instruction {

    private static final long serialVersionUID = 1L;

    private DEREF() {
    }

    /**
     * The instruction microcode
     * @param context ExecutionContext
     */
    @Override
    public void execute(final ExecutionContext context)
            throws ZExecutionException, VMExecutor.SuspendedException {
       
       Object [] vals = context.popSyncStackVals(2);
       Object ref = vals[0];
       Object relativeTo = vals[1];
       
       if (relativeTo instanceof Map) {
           context.push(((Map) relativeTo).get(ref));
       } else {
           try {
               context.push(new JavaMethodCall(relativeTo, (String) ref));
           } catch (NoSuchMethodException ex) {
               throw new ZExecutionException("invalid method " + ref + " for object " + relativeTo);
           }
       }
       context.ip++;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new DEREF();
}
