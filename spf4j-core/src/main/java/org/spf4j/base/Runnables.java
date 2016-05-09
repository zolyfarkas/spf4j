package org.spf4j.base;

import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public final class Runnables {

  private Runnables() { }


  @Nullable
  public static RuntimeException runAll(final Runnable... runnables) {
    RuntimeException ex = null;
    for (Runnable closeable : runnables) {
      try {
        closeable.run();
      } catch (RuntimeException ex1) {
        if (ex == null) {
          ex = ex1;
        } else {
          ex = Throwables.suppress(ex1, ex);
        }
      }
    }
    return ex;
  }

}
