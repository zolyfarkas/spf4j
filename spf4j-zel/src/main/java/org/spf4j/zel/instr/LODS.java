package org.spf4j.zel.instr;

import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.ZExecutionException;

/**
 * get a element from the stack referenced by the index from code param
 *
 * @author zoly
 */
public final class LODS extends Instruction {

    private static final long serialVersionUID = -8076060068156009247L;

    private LODS() {
    }

    @Override
    public void execute(final ExecutionContext context) throws ZExecutionException {
        context.push(context.getFromPtr(((Number) context.code.get(++context.ip)).intValue()));
        context.ip++;
    }
    /**
     * instance
     */
    public static final Instruction INSTANCE = new LODS();
}
