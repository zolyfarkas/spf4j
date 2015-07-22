package org.spf4j.base;

/**
 *
 * @author zoly
 */
public abstract class CheckedRunnable<EX extends Exception> extends AbstractRunnable {

    public CheckedRunnable(final boolean lenient, final String threadName) {
        super(lenient, threadName);
    }

    public CheckedRunnable(final boolean lenient) {
        super(lenient);
    }

    public CheckedRunnable() {
    }

    public CheckedRunnable(final String threadName) {
        super(threadName);
    }

    @Override
    public abstract void doRun() throws EX;


}
