
package org.spf4j.concurrent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.spf4j.base.Pair;
import org.spf4j.base.Throwables;

/**
 *
 * @author zoly
 */
public final class Futures {
  
  private Futures() { }
  
  @CheckReturnValue
  public static RuntimeException cancelAll(final boolean mayInterrupt, final Future ... futures) {
    RuntimeException ex = null;
    for (Future future : futures) {
      try {
        future.cancel(mayInterrupt);
      } catch (RuntimeException e) {
        if (ex == null) {
          ex = e;
        } else {
          ex = Throwables.suppress(ex, e);
        }
      }
    }
    return ex;
  }
 
  @CheckReturnValue
  @Nonnull
  public static Pair<Map<Future, Object>, Exception> getAll(final long timeoutMillis, final Future... futures)  {
    long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    return getAllWithDeadline(deadlineNanos, futures);
  }

  @CheckReturnValue
  @Nonnull
  public static Pair<Map<Future, Object>, Exception> getAllWithDeadline(final long deadlineNanos,
          final Future... futures) {
    Exception exception = null;
    Map<Future, Object> results = new HashMap<>(futures.length);
    for (int i = 0; i < futures.length; i++) {
      Future future = futures[i];
      try {
        final long toNanos = deadlineNanos - System.nanoTime();
        if (toNanos > 0) {
          results.put(future, future.get(toNanos, TimeUnit.NANOSECONDS));
        } else {
          throw new TimeoutException("Timed out when about to run " + future);
        }
      } catch (InterruptedException | TimeoutException ex) {
        if (exception == null) {
          exception = ex;
        } else {
          exception = Throwables.suppress(ex, exception);
        }
        int next = i + 1;
        if (next < futures.length) {
          RuntimeException cex = cancelAll(true, Arrays.copyOfRange(futures, next, futures.length));
          if (cex != null) {
            exception = Throwables.suppress(exception, cex);
          }
        }
        break;
      } catch (ExecutionException | RuntimeException ex) {
        if (exception == null) {
          exception = ex;
        } else {
          exception = Throwables.suppress(exception, ex);
        }
      }
    }
    return Pair.of(results, exception);
  }
  
}
