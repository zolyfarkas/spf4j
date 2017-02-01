
package org.spf4j.base;

import java.util.concurrent.Callable;

/**
 *
 * @author zoly
 */
public abstract class TimeoutRunnable<E extends Exception> extends CheckedRunnable<E>
    implements Callable<Void> {

    private final long deadlineMillis;

    public TimeoutRunnable(final long deadlineMillis, final boolean lenient) {
        super(lenient);
        this.deadlineMillis = deadlineMillis;
    }

    public TimeoutRunnable(final long deadlineMillis) {
        this.deadlineMillis = deadlineMillis;
    }

    @Override
    public final void doRun() throws E {
        doRun(deadlineMillis);
    }


    public abstract void doRun(long pdeadlineMillis) throws E;

    public final long getDeadlineMillis() {
        return deadlineMillis;
    }

    @Override
    public final Void call() {
        run();
        return null;
    }

}
