package org.spf4j.concurrent;

import com.google.common.annotations.Beta;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;
import org.spf4j.base.UncheckedTimeoutException;

/**
 * A Completable future that will wrap all Functions to properly propagate ExecutionContext.
 * see CompletableFuture javadoc for more detail.
 *
 * This works properly only in JDK 11.
 *
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")
@Beta
@ParametersAreNonnullByDefault
public class ContextPropagatingCompletableFuture<T>
        extends InterruptibleCompletableFuture<T> {

  private final ExecutionContext parentContext;

  private final long deadlinenanos;

  public ContextPropagatingCompletableFuture(final ExecutionContext parentContext, final long deadlinenanos) {
    this.parentContext = parentContext;
    this.deadlinenanos = deadlinenanos;
  }



  @Override
  public <U> CompletableFuture<U> thenApply(final Function<? super T, ? extends U> fn) {
    return super.thenApply(ExecutionContexts.propagatingFunction(fn, parentContext,
            null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<U> thenApplyAsync(final Function<? super T, ? extends U> fn) {
    return super.thenApplyAsync(ExecutionContexts.propagatingFunction(fn, parentContext,
            null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<U> thenApplyAsync(final Function<? super T, ? extends U> fn, final Executor executor) {
    return super.thenApplyAsync(ExecutionContexts.propagatingFunction(fn, parentContext,
            null, deadlinenanos), executor);
  }

  @Override
  public CompletableFuture<Void> thenAccept(final Consumer<? super T> action) {
    return super.thenAccept(ExecutionContexts.propagatingConsumer(action,
            parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> thenAcceptAsync(final Consumer<? super T> action) {
    return super.thenAcceptAsync(ExecutionContexts.propagatingConsumer(action,
            parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> thenAcceptAsync(final Consumer<? super T> action, final Executor executor) {
    return super.thenAcceptAsync(ExecutionContexts.propagatingConsumer(action,
            parentContext, null, deadlinenanos), executor);
  }

  @Override
  public CompletableFuture<Void> thenRun(final Runnable action) {
    return super.thenRun(ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> thenRunAsync(final Runnable action) {
    return super.thenRunAsync(
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> thenRunAsync(final Runnable action, final Executor executor) {
    return super.thenRunAsync(
                    ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos), executor);
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombine(final CompletionStage<? extends U> other,
          final BiFunction<? super T, ? super U, ? extends V> fn) {
    return super.thenCombine(other,
            ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombineAsync(final CompletionStage<? extends U> other,
          final BiFunction<? super T, ? super U, ? extends V> fn) {
    return super.thenCombineAsync(other,
            ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public <U, V> CompletableFuture<V> thenCombineAsync(final CompletionStage<? extends U> other,
          final BiFunction<? super T, ? super U, ? extends V> fn, final Executor executor) {
    return super.thenCombineAsync(other,
                    ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos),
                    executor);
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBoth(final CompletionStage<? extends U> other,
          final BiConsumer<? super T, ? super U> action) {
    return super.thenAcceptBoth(other, ExecutionContexts.propagatingBiConsumer(action, parentContext,
                    null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBothAsync(final CompletionStage<? extends U> other,
          final BiConsumer<? super T, ? super U> action) {
    return super.thenAcceptBothAsync(other,
            ExecutionContexts.propagatingBiConsumer(action, parentContext,
                    null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<Void> thenAcceptBothAsync(final CompletionStage<? extends U> other,
          final BiConsumer<? super T, ? super U> action, final Executor executor) {
    return super.thenAcceptBothAsync(other,
            ExecutionContexts.propagatingBiConsumer(action, parentContext,
                    null, deadlinenanos), executor);
  }

  @Override
  public CompletableFuture<Void> runAfterBoth(final CompletionStage<?> other, final Runnable action) {
    return super.runAfterBoth(other,
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> runAfterBothAsync(final CompletionStage<?> other, final Runnable action) {
    return super.runAfterBothAsync(other, ExecutionContexts.propagatingRunnable(action, parentContext,
            null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> runAfterBothAsync(final CompletionStage<?> other,
          final Runnable action, final Executor executor) {
    return super.runAfterBothAsync(other, ExecutionContexts.propagatingRunnable(action, parentContext,
            null, deadlinenanos), executor);
  }

  @Override
  public <U> CompletableFuture<U> applyToEither(final CompletionStage<? extends T> other,
          final Function<? super T, U> fn) {
    return super.applyToEither(other,
                    ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<U> applyToEitherAsync(final CompletionStage<? extends T> other,
          final Function<? super T, U> fn) {
    return super.applyToEitherAsync(other,
            ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<U> applyToEitherAsync(final CompletionStage<? extends T> other,
          final Function<? super T, U> fn, final Executor executor) {
    return super.applyToEitherAsync(other,
            ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos), executor);
  }

  @Override
  public CompletableFuture<Void> acceptEither(final CompletionStage<? extends T> other,
          final Consumer<? super T> action) {
    return super.acceptEither(other,
            ExecutionContexts.propagatingConsumer(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> acceptEitherAsync(final CompletionStage<? extends T> other,
          final Consumer<? super T> action) {
    return super.acceptEitherAsync(other,
            ExecutionContexts.propagatingConsumer(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> acceptEitherAsync(final CompletionStage<? extends T> other,
          final Consumer<? super T> action, final Executor executor) {
    return super.acceptEitherAsync(other,
            ExecutionContexts.propagatingConsumer(action, parentContext, null, deadlinenanos), executor);
  }

  @Override
  public CompletableFuture<Void> runAfterEither(final CompletionStage<?> other, final Runnable action) {
    return super.runAfterEither(other,
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> runAfterEitherAsync(final CompletionStage<?> other, final Runnable action) {
    return super.runAfterEitherAsync(other,
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<Void> runAfterEitherAsync(final CompletionStage<?> other,
          final Runnable action, final Executor executor) {
    return super.runAfterEitherAsync(other,
            ExecutionContexts.propagatingRunnable(action, parentContext, null, deadlinenanos), executor);
  }

  @Override
  public <U> CompletableFuture<U> thenCompose(final Function<? super T, ? extends CompletionStage<U>> fn) {
    return super.thenCompose(ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn) {
    return super.thenComposeAsync(ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn,
          final Executor executor) {
    return super.thenComposeAsync(
            ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos), executor);
  }

  @Override
  public CompletableFuture<T> exceptionally(final Function<Throwable, ? extends T> fn) {
    return super.exceptionally(ExecutionContexts.propagatingFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action) {
    return super.whenComplete(ExecutionContexts.propagatingBiConsumer(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action) {
    return super.whenCompleteAsync(
            ExecutionContexts.propagatingBiConsumer(action, parentContext, null, deadlinenanos));
  }

  @Override
  public CompletableFuture<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action,
          final Executor executor) {
    return super.whenCompleteAsync(
            ExecutionContexts.propagatingBiConsumer(action, parentContext, null, deadlinenanos),
            executor);
  }

  @Override
  public <U> CompletableFuture<U> handle(final BiFunction<? super T, Throwable, ? extends U> fn) {
    return super.handle(ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn) {
    return super.handleAsync(
            ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos));
  }

  @Override
  public <U> CompletableFuture<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn,
          final Executor executor) {
    return super.handleAsync(
            ExecutionContexts.propagatingBiFunction(fn, parentContext, null, deadlinenanos), executor);
  }

  @Override
  public CompletableFuture<T> toCompletableFuture() {
    return this;
  }

  @Override
  public String toString() {
    return "ContextPropagatingCompletableFuture{" + "parentContext=" + parentContext
            + ", deadlinenanos=" + deadlinenanos + ", super=" + super.toString() +  '}';
  }

  @Override
  @Nullable
  public T get() throws InterruptedException, ExecutionException {
    try {
      long timeout = deadlinenanos - TimeSource.nanoTime();
      if (timeout < 0) {
        throw new UncheckedTimeoutException("deadline exceeded " + Timing.getCurrentTiming()
                .fromNanoTimeToInstant(deadlinenanos));
      }
      return super.get(timeout, TimeUnit.NANOSECONDS);
    } catch (TimeoutException ex) {
      throw new UncheckedTimeoutException(ex);
    }
  }


  /**
   * JDK 11.
   */
  public CompletableFuture<T> completeAsync(final Supplier<? extends T> supplier) {
    try {
      return (CompletableFuture)
              this.getClass().getMethod("completeAsync", new Class[] {Supplier.class})
                .invoke(this, ExecutionContexts.propagatingSupplier(supplier, parentContext, null, deadlinenanos));
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ex) {
      throw new UnsupportedOperationException("Supported only on JDK 11", ex);
    }
  }

  /**
   * JDK 11.
   */
  public CompletableFuture<T> completeAsync(final Supplier<? extends T> supplier, final Executor executor) {
    try {
      return (CompletableFuture)
              this.getClass().getMethod("completeAsync", new Class[] {Supplier.class, Executor.class})
                      .invoke(this,
                              ExecutionContexts.propagatingSupplier(supplier, parentContext, null, deadlinenanos),
                              executor);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ex) {
      throw new UnsupportedOperationException("Supported only on JDK 11", ex);
    }
  }

  /**
   * JDK 11!
   */
  public <U> CompletableFuture<U> newIncompleteFuture() {
    return new ContextPropagatingCompletableFuture<>(parentContext, deadlinenanos);
  }

  public static <U> CompletableFuture<U> supplyAsync(final Executor e, final Supplier<U> f) {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return CompletableFuture.supplyAsync(f, e);
    }
    return supplyAsync(current, e, f, current.getDeadlineNanos());
  }

  public static <U> CompletableFuture<U> supplyAsync(
          final Executor e, final Supplier<U> f, final long deadlineNanos) {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return CompletableFuture.supplyAsync(f, e);
    }
    return supplyAsync(current, e, f, deadlineNanos);
  }

  public static <U> CompletableFuture<U> supplyAsync(final ExecutionContext current,
          final Executor e, final Supplier<U> f, final long deadlineNanos) {
    ContextPropagatingCompletableFuture<U> d
            = new ContextPropagatingCompletableFuture<U>(current, current.getDeadlineNanos());
    e.execute(ExecutionContexts.propagatingRunnable(() -> {
      try {
        d.complete(f.get());
      } catch (Throwable t) {
        d.completeExceptionally(t);
      }
    }, current, f.toString(), deadlineNanos));
    return d;
  }

  public static <U> CompletableFuture<U> completedFuture(final U value) {
    ExecutionContext current = ExecutionContexts.current();
    if (current == null) {
      return CompletableFuture.completedFuture(value);
    } else {
      return completedFuture(current, current.getDeadlineNanos(), value);
    }
  }

  public static <U> CompletableFuture<U> completedFuture(
          final ExecutionContext parentContext, final long deadlinenanos, final U value) {
    ContextPropagatingCompletableFuture<U> r = new ContextPropagatingCompletableFuture<U>(parentContext, deadlinenanos);
    if (!r.complete(value)) {
      throw new IllegalStateException("Cannot be already completed " + r);
    }
    return r;
  }


}
