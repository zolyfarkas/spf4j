
package org.spf4j.zel.instr;

import java.util.concurrent.Callable;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Program;

public final class SyncAsyncCallable implements Callable<Object> {

    private final ExecutionContext nctx;

    public SyncAsyncCallable(final ExecutionContext nctx) {
        this.nctx = nctx;
    }

    @Override
    public Object call() throws Exception {
        return Program.executeSyncOrAsync(nctx);
    }
}
