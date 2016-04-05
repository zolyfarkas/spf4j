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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.zel.vm.AssignableValue;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Program;
import org.spf4j.zel.vm.ZExecutionException;


@SuppressFBWarnings("CD_CIRCULAR_DEPENDENCY")
public final class LODAX extends Instruction implements LValRef {

    private static final long serialVersionUID = 1257172216541960034L;

    private final String symbol;
    
    public LODAX(final String symbol) {
        this.symbol = symbol;
    }
    
    
    public static void writeTo(final String symbol, final ExecutionContext context, final Object what)
            throws ZExecutionException {
        Program code = context.getProgram();
        Integer addr  = code.getLocalSymbolTable().get(symbol);
        if (addr == null) {
            addr = code.getGlobalSymbolTable().get(symbol);
            if (addr == null) {
                throw new ZExecutionException("unalocated symbol encountered " + symbol);
            }
            context.globalPoke(addr, what);
        } else {
            context.localPoke(addr, what);
        }
    }

    @Override
    public int execute(final ExecutionContext context) {
           
        context.push(new AssignableValue() {

            @Override
            public void assign(final Object object) throws ZExecutionException {
                writeTo(symbol, context, object);
            }

            @Override
            public Object get() throws ZExecutionException {
                return LODX.readFrom(symbol, context);
            }
        });
        return 1;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {symbol};
    }

}
