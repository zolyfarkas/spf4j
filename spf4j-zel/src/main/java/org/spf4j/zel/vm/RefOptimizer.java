/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 *
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
        Instruction [] instructions = input.getInstructions().clone();
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

        try {
            return new Program(input.getGlobalSymbolTable(),
                    input.getGlobalMem(), input.getLocalSymbolTable(),
                    instructions, input.getDebug(), input.getSource(),
                    input.getType(), input.getExecType(),
                    input.hasDeterministicFunctions());
        } catch (CompileException ex) {
            throw new RuntimeException(ex);
        }
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
