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

package org.spf4j.zel.vm;

import com.google.common.base.Function;
import java.util.Map;
import org.spf4j.zel.instr.Instruction;
import org.spf4j.zel.instr.LODAX;
import org.spf4j.zel.instr.LODAXF;
import org.spf4j.zel.instr.LODX;
import org.spf4j.zel.instr.LODXF;

/**
 * Changes reference by name instructions to reference by index.
 * @author zoly
 */
public final class RefOptimizer implements Function<Program, Program> {

    private RefOptimizer() { }

    public static final Function<Program, Program> INSTANCE = new RefOptimizer();

    @Override
    public Program apply(final Program input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null for " + this);
        }
        Instruction[] instructions = input.getInstructions().clone();
        Map<String, Integer> lsym = input.getLocalSymbolTable();
        Map<String, Integer> gsym = input.getGlobalSymbolTable();

        for (int i = 0; i < instructions.length; i++) {
            Instruction instr = instructions[i];
            if (LODX.class.equals(instr.getClass())) {
                String symbol = ((LODX) instr).getSymbol();
                Address addr = getAddress(lsym, symbol, gsym);
                instructions[i] = new LODXF(addr);
            } else if (LODAX.class.equals(instr.getClass())) {
                String symbol = ((LODAX) instr).getSymbol();
                Address addr = getAddress(lsym, symbol, gsym);
                instructions[i] = new LODAXF(addr);
            }
        }

        return new Program(input.getName(), input.getGlobalSymbolTable(),
                    input.getGlobalMem(), input.getLocalSymbolTable(),
                    instructions, input.getDebug(), input.getSource(),
                    input.getType(), input.getExecType(),
                    input.hasDeterministicFunctions());
    }

    private static Address getAddress(final Map<String, Integer> lsym,
            final String symbol, final Map<String, Integer> gsym) {
        Address addr;
        Integer idx = lsym.get(symbol);
        if (idx == null) {
            idx = gsym.get(symbol);
            addr = new Address(idx, Address.Scope.GLOBAL);
        } else {
            addr = new Address(idx, Address.Scope.LOCAL);
        }
        return addr;
    }

}
