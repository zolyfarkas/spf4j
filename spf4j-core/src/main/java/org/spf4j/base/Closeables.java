package org.spf4j.base;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public final class Closeables {

  private Closeables() {
  }

  @Nullable
  @CheckReturnValue
  public static Exception closeAll(final AutoCloseable... closeables) {
    return closeAll(null, closeables);
  }

  @Nullable
  @CheckReturnValue
  public static Exception closeAll(@Nullable final Exception propagate, final AutoCloseable... closeables) {
    Exception ex = propagate;
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
  @CheckReturnValue
  public static IOException closeAll(final Closeable... closeables) {
    return closeAll(null, closeables);
  }

  @Nullable
  @CheckReturnValue
  public static IOException closeAll(@Nullable final Exception propagate, final Closeable... closeables) {
    IOException ex;
    if (propagate == null) {
      ex = null;
    } else if (propagate instanceof IOException) {
      ex = (IOException) propagate;
    } else {
      ex = new IOException(propagate);
    }
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
  @CheckReturnValue
  public static Exception closeAll(final Iterable<? extends AutoCloseable> closeables) {
    return closeAll(null, closeables);
  }

  @Nullable
  @CheckReturnValue
  public static Exception closeAll(@Nullable final Exception propagate,
          final Iterable<? extends AutoCloseable> closeables) {
    Exception ex = propagate;
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
  @CheckReturnValue
  public static IOException closeSelectorChannels(final Selector selector) {
    return closeSelectorChannels(null, selector);
  }

  @Nullable
  @CheckReturnValue
  public static IOException closeSelectorChannels(@Nullable final IOException propagate, final Selector selector) {
    IOException ex = propagate;
    for (SelectionKey key : selector.keys()) {
      SelectableChannel channel = key.channel();
      try {
        channel.close();
      } catch (IOException ex2) {
        if (ex == null) {
          ex = ex2;
        } else {
          ex = Throwables.suppress(ex, ex2);
        }
      }
    }
    return ex;
  }

}
