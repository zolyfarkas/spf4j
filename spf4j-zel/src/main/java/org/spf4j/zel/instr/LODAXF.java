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

import org.spf4j.zel.vm.Address;
import org.spf4j.zel.vm.AssignableValue;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.ZExecutionException;


public final class LODAXF extends Instruction {

    private static final long serialVersionUID = 1257172216541960034L;

    private final Address fromAddr;
    
    public LODAXF(final Address fromAddr) {
        this.fromAddr = fromAddr;
    }

    @Override
    public int execute(final ExecutionContext context) {
        context.push(new AssignableValue() {

            @Override
            public void assign(final Object object) throws ZExecutionException {
                if (fromAddr.getScope() == Address.Scope.LOCAL) {
                  context.localPoke(fromAddr.getAddress(), object);
                } else {
                    throw new ZExecutionException("Cannot assign " + object + "to a global refference");
                }
            }

            @Override
            public Object get() {
                if (fromAddr.getScope() == Address.Scope.LOCAL) {
                    return context.localPeek(fromAddr.getAddress());
                } else {
                    return context.globalPeek(fromAddr.getAddress());
                }
            }
        });
        return 1;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {fromAddr};
    }

}
