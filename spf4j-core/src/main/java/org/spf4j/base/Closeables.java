package org.spf4j.base;

import java.io.Closeable;
import java.io.IOException;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public final class Closeables {

  private Closeables() {
  }

  @Nullable
  public static Exception closeAll(final AutoCloseable... closeables) {
    Exception ex = null;
    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception ex1) {
        if (ex == null) {
          ex = ex1;
        } else {
          ex = Throwables.suppress(ex1, ex);
        }
      }
    }
    return ex;
  }

  @Nullable
  public static IOException closeAll(final Closeable... closeables) {
    IOException ex = null;
    for (Closeable closeable : closeables) {
      try {
        closeable.close();
      } catch (IOException ex1) {
        if (ex == null) {
          ex = ex1;
        } else {
          ex = Throwables.suppress(ex1, ex);
        }
      }
    }
    return ex;
  }

  @Nullable
  public static Exception closeAll(final Iterable<? extends AutoCloseable> closeables) {
    Exception ex = null;
    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception ex1) {
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
