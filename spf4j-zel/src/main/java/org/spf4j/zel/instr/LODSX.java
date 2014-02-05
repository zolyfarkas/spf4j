package org.spf4j.zel.instr;

import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.ZExecutionException;

/**
 * get a element from the stack referenced by the index from the top of stack and put it on top of the stack
 *
 * @author zoly
 */
public final class LODSX extends Instruction {

    private static final long serialVersionUID = 1L;

    private LODSX() {
    }

    public void execute(ExecutionContext context) throws ZExecutionException {
        Number idx = (Number) context.pop();
        context.push(context.getFromPtr(idx.intValue()));
        context.ip++;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new LODSX();
}
