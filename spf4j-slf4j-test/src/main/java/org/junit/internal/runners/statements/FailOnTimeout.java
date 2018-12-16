/**
 * overwrite this class to  disable this behavior when debugger is on.
 */
package org.junit.internal.runners.statements;

import org.junit.runners.model.Statement;
import org.spf4j.test.log.TestUtils;

public final class FailOnTimeout extends Statement {

  private final Statement fNext;

  private final long fTimeout;

  private volatile boolean fFinished = false;

  private volatile Throwable fThrown = null;

  public FailOnTimeout(final Statement next, final long timeout) {
    fNext = next;
    fTimeout = timeout;
  }

  @Override
  public void evaluate() throws Throwable {
    if (TestUtils.isExecutedWithDebuggerAgent()) {
      fNext.evaluate();
      return;
    }
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          fNext.evaluate();
          fFinished = true;
        } catch (Throwable e) {
          fThrown = e;
        }
      }
    };
    thread.start();
    thread.join(fTimeout);
    if (fFinished) {
      return;
    }
    if (fThrown != null) {
      throw fThrown;
    }
    Exception exception = new Exception(String.format(
            "test timed out after %d milliseconds", fTimeout));
    exception.setStackTrace(thread.getStackTrace());
    throw exception;
  }

  @Override
  public String toString() {
    return "FailOnTimeout{" + "fNext=" + fNext + ", fTimeout=" + fTimeout + ", fFinished="
            + fFinished + ", fThrown=" + fThrown + '}';
  }


}
