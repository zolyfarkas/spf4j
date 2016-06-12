
package org.spf4j.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
  public static RuntimeException cancelAll(final boolean mayInterrupt, final Iterator<Future> iterator) {
    RuntimeException ex = null;
    while (iterator.hasNext()) {
      Future future = iterator.next();
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
    return getAllWithDeadlineNanos(deadlineNanos, futures);
  }

  /**
   * Gets all futures resuls.
   *
   * @param deadlineNanos
   * @param futures
   * @return
   */
  @CheckReturnValue
  @Nonnull
  public static Pair<Map<Future, Object>, Exception> getAllWithDeadlineNanos(final long deadlineNanos,
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

  @CheckReturnValue
  @Nonnull
  public static Pair<Map<Future, Object>, Exception> getAll(final long timeoutMillis, final Iterable<Future> futures)  {
    long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    return getAllWithDeadlineNanos(deadlineNanos, futures);
  }


  @CheckReturnValue
  @Nonnull
  public static Pair<Map<Future, Object>, Exception> getAllWithDeadlineNanos(final long deadlineNanos,
          final Iterable<Future> futures) {
    Exception exception = null;
    Map<Future, Object> results;
    if (futures instanceof Collection) {
      results = new HashMap<>(((Collection) futures).size());
    } else {
      results = new HashMap<>();
    }
    Iterator<Future> iterator = futures.iterator();
    while (iterator.hasNext()) {
      Future future = iterator.next();
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
        RuntimeException cex = cancelAll(true, iterator);
        if (cex != null) {
          exception = Throwables.suppress(exception, cex);
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
