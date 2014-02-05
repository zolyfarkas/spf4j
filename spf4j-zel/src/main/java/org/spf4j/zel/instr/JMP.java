package org.spf4j.zel.instr;

import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.ZExecutionException;

public final class JMP extends Instruction {

    private static final long serialVersionUID = -3763337334172149636L;

    private JMP() {
    }

    public void execute(ExecutionContext context) throws ZExecutionException {
        int address = ((Number) context.code.get(++context.ip)).intValue();
        context.ip = address;

    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new JMP();
}
