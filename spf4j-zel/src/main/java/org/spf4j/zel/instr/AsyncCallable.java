
package org.spf4j.zel.instr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Callable;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Program;

@SuppressFBWarnings("CD_CIRCULAR_DEPENDENCY")
public final class AsyncCallable implements Callable<Object> {

    private final ExecutionContext nctx;

    public AsyncCallable(final ExecutionContext nctx) {
        this.nctx = nctx;
    }

    @Override
    public Object call() throws Exception {
        return Program.executeAsync(nctx);
    }

    @Override
    public String toString() {
        return "AsyncCallable{" + "nctx=" + nctx + '}';
    }

}
