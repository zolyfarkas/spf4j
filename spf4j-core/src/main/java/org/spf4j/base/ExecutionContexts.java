/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.base;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Signed;
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.ExecutionContext.Relation;
import org.spf4j.concurrent.ScalableSequence;
import org.spf4j.concurrent.UIDGenerator;
import org.spf4j.ds.SimpleStack;

/**
 * @author Zoltan Farkas
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public final class ExecutionContexts {

  private static final UIDGenerator ID_GEN = new UIDGenerator(new ScalableSequence(0, 10), "X", 1544368928196L);

  public static final long DEFAULT_TIMEOUT_NANOS
          = Long.getLong("spf4j.execContext.defaultTimeoutNanos", TimeUnit.HOURS.toNanos(8));

  private static final ThreadLocal<SimpleStack<ExecutionContext>> EXEC_CTX =
          new ThreadLocal<SimpleStack<ExecutionContext>>() {
    @Override
    protected SimpleStack<ExecutionContext> initialValue() {
      return new SimpleStack<>(4);
    }

  };

  private static final ThreadLocalContextAttacher DEFAULT_TL_ATTACHER = new ThreadLocalScopeImpl();

  private static final ExecutionContextFactory<ExecutionContext> CTX_FACTORY = initFactory();

  private static final ThreadLocalContextAttacher TL_ATTACHER =  initTLAttacher();

  private ExecutionContexts() {
  }

  private static ThreadLocalContextAttacher initTLAttacher() {
    String factoryClass = System.getProperty("spf4j.execContext.tlAttacherClass");
    ThreadLocalContextAttacher factory;
    if (factoryClass == null) {
      factory = DEFAULT_TL_ATTACHER;
    } else {
      try {
        factory = ((Class<ThreadLocalContextAttacher>) Class.forName(factoryClass)).newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
    return factory;
  }

 private static ExecutionContextFactory<ExecutionContext> initFactory() {
    String factoryClass = System.getProperty("spf4j.execContext.factoryClass");
    ExecutionContextFactory<ExecutionContext> factory;
    if (factoryClass == null) {
      factory = new BasicExecutionContextFactory();
    } else {
      try {
        factory = ((Class<ExecutionContextFactory<ExecutionContext>>) Class.forName(factoryClass)).newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
    String factoryWrapperClass = System.getProperty("spf4j.execContext.factoryWrapperClass");
    if (factoryWrapperClass != null) {
      try {
        factory = (ExecutionContextFactory<ExecutionContext>) Class.forName(factoryWrapperClass)
                .getConstructor(ExecutionContextFactory.class).newInstance(factory);
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
              | NoSuchMethodException | InvocationTargetException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
    return factory;
  }

  public static ThreadLocalContextAttacher defaultThreadLocalAttacher() {
    return DEFAULT_TL_ATTACHER;
  }

  public static ThreadLocalContextAttacher threadLocalAttacher() {
    return TL_ATTACHER;
  }

  public static CharSequence genId() {
    return ID_GEN.next();
  }

  public static ExecutionContextFactory<ExecutionContext> getContextFactory() {
    return CTX_FACTORY;
  }

  @Nullable
  public static ExecutionContext current() {
    return EXEC_CTX.get().peek();
  }

  @Nullable
  public static void clearCurrentThread() {
     EXEC_CTX.get().clear();
  }

  /**
   * start a execution context.
   *
   * @param deadlineNanos the deadline for this context. (System.nanotime)
   * @return the execution context.
   */
  public static ExecutionContext start(final long startTimeNanos, final long deadlineNanos) {
    return start("anon", null, startTimeNanos, deadlineNanos);
  }

  /**
   * start a execution context.
   *
   * @param timeout
   * @param tu
   * @return
   */
  public static ExecutionContext start(final long timeout, final TimeUnit tu) {
    return start("anon", current(), timeout, tu);
  }

  public static ExecutionContext start(final String opname) {
    return start(opname, current(), DEFAULT_TIMEOUT_NANOS, TimeUnit.NANOSECONDS);
  }

  public static ExecutionContext start(final String opname, final long timeout, final TimeUnit tu) {
    return start(opname, current(), timeout, tu);
  }

  public static ExecutionContext start(@Nullable final ExecutionContext parent, final long timeout, final TimeUnit tu) {
    return start("anon", parent, timeout, tu);
  }

  public static ExecutionContext start(@Nullable final ExecutionContext parent) {
    long nanoTime = TimeSource.nanoTime();
    return start(parent, nanoTime, parent != null ? parent.getDeadlineNanos() : nanoTime + DEFAULT_TIMEOUT_NANOS);
  }

  public static ExecutionContext start(@Nullable final ExecutionContext parent,
          final long startTimeNanos, final long deadlineNanos) {
    return start("anon", parent, startTimeNanos, deadlineNanos);
  }

  public static ExecutionContext start(final String name, final long startTimeNanos, final long deadlineNanos) {
    return start(name, current(), startTimeNanos, deadlineNanos);
  }

  public static ExecutionContext start(final String name, final long deadlineNanos) {
    return start(name, current(), TimeSource.nanoTime(), deadlineNanos);
  }

  public static ExecutionContext start(final String name,
          @Nullable final ExecutionContext parent) {
    long nanoTime = TimeSource.nanoTime();
    return start(name, parent, nanoTime, parent != null ? parent.getDeadlineNanos()
            : nanoTime + DEFAULT_TIMEOUT_NANOS);
  }

  public static ExecutionContext start(final String name,
          @Nullable final ExecutionContext parent, final long timeout, final TimeUnit tu) {
    return start(name, null, parent, timeout, tu);
  }

  public static ExecutionContext start(final String name, @Nullable final CharSequence id,
          @Nullable final ExecutionContext parent, final long timeout, final TimeUnit tu) {
    long nanoTime = TimeSource.nanoTime();
    return start(name, id, parent, nanoTime, computeDeadline(nanoTime, parent, tu, timeout));
  }

  public static ExecutionContext createDetached(final String name,
          @Nullable final ExecutionContext parent, final long timeout, final TimeUnit tu) {
    long nanoTime = TimeSource.nanoTime();
    return createDetached(name, parent, nanoTime, computeDeadline(nanoTime, parent, tu, timeout));
  }

  public static ExecutionContext start(final String name,
          @Nullable final ExecutionContext parent, final long deadlineNanos) {
    return start(name, parent, TimeSource.nanoTime(), deadlineNanos);
  }

  public static ExecutionContext start(final String name,
          @Nullable final ExecutionContext parent, final long startTimeNanos, final long deadlineNanos) {
    return start(name, null, parent, startTimeNanos, deadlineNanos);
  }

  public static ExecutionContext start(final String name, @Nullable final CharSequence id,
          @Nullable final ExecutionContext parent, final long startTimeNanos, final long deadlineNanos) {
    return start(name, id, parent, Relation.CHILD_OF, startTimeNanos, deadlineNanos);
  }

  public static ExecutionContext start(final String name, @Nullable final CharSequence id,
          @Nullable final ExecutionContext parent, final Relation relation,
          final long startTimeNanos, final long deadlineNanos) {
    ExecutionContext nCtx = CTX_FACTORY.start(name, id, parent, relation,
              startTimeNanos, deadlineNanos);
    nCtx.attach();
    return nCtx;
  }

  public static ExecutionContext createDetached(final String name,
          @Nullable final ExecutionContext parent, final long startTimeNanos, final long deadlineNanos) {
    return createDetached(name, parent, Relation.CHILD_OF, startTimeNanos, deadlineNanos);
  }

  public static ExecutionContext createDetached(final String name, @Nullable final ExecutionContext parent,
          final Relation relation, final long startTimeNanos, final long deadlineNanos) {
    return CTX_FACTORY.start(name, null, parent, relation, startTimeNanos, deadlineNanos);
  }


  public static long getContextDeadlineNanos() {
    ExecutionContext ec = ExecutionContexts.current();
    if (ec == null) {
      return TimeSource.nanoTime() + DEFAULT_TIMEOUT_NANOS;
    } else {
      return ec.getDeadlineNanos();
    }
  }

  public static long getContextDeadlineNanos(final long currentTime) {
    ExecutionContext ec = ExecutionContexts.current();
    if (ec == null) {
      return currentTime + DEFAULT_TIMEOUT_NANOS;
    } else {
      return ec.getDeadlineNanos();
    }
  }

  @Signed
  public static long getTimeRelativeToDeadline(final TimeUnit unit) {
    return unit.convert(getContextDeadlineNanos() - TimeSource.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Nonnegative
  public static long getTimeToDeadline(final TimeUnit unit) throws TimeoutException {
    long timeRelativeToDeadline = getTimeRelativeToDeadline(unit);
    if (timeRelativeToDeadline <= 0) {
      throw new TimeoutException("Deadline exceeded by " + (-timeRelativeToDeadline) + ' ' + unit);
    }
    return timeRelativeToDeadline;
  }

  @Nonnegative
  public static int getTimeToDeadlineInt(final TimeUnit unit) throws TimeoutException {
    long timeRelativeToDeadline = getTimeToDeadline(unit);
    if (timeRelativeToDeadline > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) timeRelativeToDeadline;
    }
  }

  @Nonnegative
  public static long getMillisToDeadline() throws TimeoutException {
    return getTimeToDeadline(TimeUnit.MILLISECONDS);
  }

  @Nonnegative
  public static int getSecondsToDeadline() throws TimeoutException {
    long secondsToDeadline = getTimeToDeadline(TimeUnit.SECONDS);
    if (secondsToDeadline > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) secondsToDeadline;
    }
  }

  public static long computeDeadline(final long timeout, final TimeUnit unit) {
    return computeDeadline(current(), timeout, unit);
  }

  public static long computeTimeout(final long timeout, final TimeUnit unit) throws TimeoutException {
    return unit.convert(computeTimeoutDeadline(current(), unit, timeout).getTimeoutNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * @deprecated use variant where the value and unit are it the natural order.
   */
  @Deprecated
  public static long computeDeadline(@Nullable final ExecutionContext current,
           final TimeUnit unit, final long timeout) {
    return computeDeadline(current, timeout, unit);
  }

  public static long computeDeadline(@Nullable final ExecutionContext current,
          final long timeout, final TimeUnit unit) {
    if (current == null) {
      return TimeSource.getDeadlineNanos(timeout, unit);
    }
    long nanoTime = TimeSource.nanoTime();
    long ctxDeadlinenanos = current.getDeadlineNanos();
    long timeoutNanos = unit.toNanos(timeout);
    return (ctxDeadlinenanos - nanoTime < timeoutNanos) ? ctxDeadlinenanos : nanoTime + timeoutNanos;
  }

  public static long computeDeadline(final long startTimeNanos, @Nullable final ExecutionContext current,
          final TimeUnit unit, final long timeout) {
    if (current == null) {
      return TimeSource.getDeadlineNanos(startTimeNanos, timeout, unit);
    }
    long ctxDeadlinenanos = current.getDeadlineNanos();
    long timeoutNanos = unit.toNanos(timeout);
    return (ctxDeadlinenanos - startTimeNanos < timeoutNanos) ? ctxDeadlinenanos : startTimeNanos + timeoutNanos;
  }

  /**
   * Compute the actual timeout taking in consideration the context deadline.
   * @param current the context
   * @param unit timeout unit
   * @param timeout timeout value
   * @return the earliest timeout (of the provided and context one)
   * @throws TimeoutException
   */
  public static TimeoutDeadline computeTimeoutDeadline(@Nullable final ExecutionContext current,
          final TimeUnit unit, final long timeout) throws TimeoutException {
    if (current == null) {
      return TimeoutDeadline.of(unit.toNanos(timeout), TimeSource.getDeadlineNanos(timeout, unit));
    }
    long nanoTime = TimeSource.nanoTime();
    long ctxDeadlinenanos = current.getDeadlineNanos();
    long timeoutNanos = unit.toNanos(timeout);
    long contextTimeoutNanos = ctxDeadlinenanos - nanoTime;
    return (contextTimeoutNanos < timeoutNanos)
            ? TimeoutDeadline.of(contextTimeoutNanos, ctxDeadlinenanos)
            : TimeoutDeadline.of(timeoutNanos, nanoTime + timeoutNanos);
  }

  private static class BasicExecutionContextFactory implements ExecutionContextFactory<ExecutionContext> {

    @Override
    public ExecutionContext start(final String name, @Nullable final CharSequence id,
            @Nullable final ExecutionContext parent, final Relation relation,
            final long startTimeNanos, final long deadlineNanos) {
      return new BasicExecutionContext(name, id, parent, relation, startTimeNanos, deadlineNanos);
    }

  }

  public static <T> Callable<T> propagatingCallable(final Callable<T> callable) {
    ExecutionContext current = current();
    return current == null ? callable : propagatingCallable(callable, current);
  }

  public static <T> Callable<T> propagatingCallable(final Callable<T> callable, final ExecutionContext ctx) {
    return new PropagatingCallable<T>(callable, ctx);
  }

  public static <T> Collection<? extends Callable<T>> propagatingCallables(
          final Collection<? extends Callable<T>> tasks) {
    ExecutionContext current = current();
    return current == null ? tasks : propagatingCallables(tasks, current);
  }

  public static <T> Collection<? extends Callable<T>> propagatingCallables(
          final Collection<? extends Callable<T>> tasks,
          final ExecutionContext ctx) {
    return tasks.stream().map(
            (c) -> new PropagatingCallable<>(c, ctx))
            .collect(Collectors.toCollection(() -> new ArrayList<>(tasks.size())));
  }

  public static <T> Collection<? extends Callable<T>> deadlinedPropagatingCallables(
          final Collection<? extends Callable<T>> tasks,
          final ExecutionContext ctx, final long deadlineNanos) {
    return tasks.stream().map(
            (c) -> new PropagatingNamedCallable<>(c, ctx, null, deadlineNanos))
            .collect(Collectors.toCollection(() -> new ArrayList<>(tasks.size())));
  }

  public static <T> Callable<T> deadlinedPropagatingCallable(final Callable<T> callable,
          final ExecutionContext ctx, final long deadlineNanos) {
    return new PropagatingNamedCallable<T>(callable, ctx, null, deadlineNanos);
  }

  public static Runnable propagatingRunnable(final Runnable runnable) {
    ExecutionContext current = current();
    return current == null ? runnable : propagatingRunnable(runnable, current);
  }

  public static Runnable propagatingRunnable(final Runnable runnable, final ExecutionContext ctx) {
    return new PropagatingRunnable(runnable, ctx, null, ctx.getDeadlineNanos());
  }

  public static Runnable propagatingRunnable(final Runnable runnable, final ExecutionContext ctx,
          final String name, final long deadlineNanos) {
    return new PropagatingRunnable(runnable, ctx, name, deadlineNanos);
  }

  private static final class PropagatingCallable<T> implements Callable<T> {

    private final Callable<T> task;
    private final ExecutionContext current;

    PropagatingCallable(final Callable<T> task, final ExecutionContext current) {
      this.task = task;
      this.current = current;
    }

    @Override
    public T call() throws Exception {
      try (ExecutionContext ctx = current.startChild(task.toString())) {
        return task.call();
      }
    }
  }

  public static <T> Callable<T> propagatingCallable(final Callable<T> callable, final ExecutionContext ctx,
          final String name, final long deadlineNanos) {
    return new PropagatingNamedCallable<T>(callable, ctx, name, deadlineNanos);
  }


  private static final class PropagatingNamedCallable<T> implements Callable<T>, Wrapper<Callable<T>> {

    private final Callable<T> task;
    private final ExecutionContext current;

    private final String name;

    private final long deadlineNanos;

    PropagatingNamedCallable(final Callable<T> task, final ExecutionContext current,
            @Nullable final String name, final long deadlineNanos) {
      this.task = task;
      this.current = current;
      this.name = name;
      this.deadlineNanos = deadlineNanos;
    }

    @Override
    public T call() throws Exception {
      try (ExecutionContext ctx = start(name, current, deadlineNanos)) {
        return task.call();
      }
    }

    @Override
    public String toString() {
      return  name == null ? task.toString() : name;
    }

    @Override
    public Callable<T> getWrapped() {
      return task;
    }

  }

  public static <X, Y> Function<X, Y> propagatingFunction(final Function<X, Y> callable, final ExecutionContext ctx,
          final String name, final long deadlineNanos) {
    return new PropagatingFunction<X, Y>(callable, ctx, name, deadlineNanos);
  }

  private static final class PropagatingFunction<X, Y> implements Function<X, Y>, Wrapper<Function<X, Y>> {

    private final Function<X, Y> function;
    private final ExecutionContext current;

    private final String name;

    private final long deadlineNanos;

    PropagatingFunction(final Function<X, Y> task, final ExecutionContext current,
            final String name, final long deadlineNanos) {
      this.function = task;
      this.current = current;
      this.name = name;
      this.deadlineNanos = deadlineNanos;
    }

    @Override
    public Y apply(final X in) {
      try (ExecutionContext ctx = start(name, current, deadlineNanos)) {
        return function.apply(in);
      }
    }

    @Override
    public String toString() {
      return name == null ? function.toString() : name;
    }

    @Override
    public Function<X, Y> getWrapped() {
      return function;
    }

  }

  public static <X, Y, Z> BiFunction<X, Y, Z> propagatingBiFunction(final BiFunction<X, Y, Z> callable,
          final ExecutionContext ctx,
          final String name, final long deadlineNanos) {
    return new PropagatingBiFunction<X, Y, Z>(callable, ctx, name, deadlineNanos);
  }

 private static final class PropagatingBiFunction<X, Y, Z>
         implements BiFunction<X, Y, Z>, Wrapper<BiFunction<X, Y, Z>> {

    private final BiFunction<X, Y, Z> function;
    private final ExecutionContext current;

    private final String name;

    private final long deadlineNanos;

    PropagatingBiFunction(final BiFunction<X, Y, Z> task, final ExecutionContext current,
            final String name, final long deadlineNanos) {
      this.function = task;
      this.current = current;
      this.name = name;
      this.deadlineNanos = deadlineNanos;
    }

    @Override
    public Z apply(final X x, final Y y) {
      try (ExecutionContext ctx = start(name, current, deadlineNanos)) {
        return function.apply(x, y);
      }
    }

    @Override
    public String toString() {
      return name == null ? function.toString() : name;
    }

    @Override
    public BiFunction<X, Y, Z> getWrapped() {
      return function;
    }

  }


  public static <X> Consumer<X> propagatingConsumer(final Consumer<X> callable, final ExecutionContext ctx,
          final String name, final long deadlineNanos) {
    return new PropagatingConsumer<X>(callable, ctx, name, deadlineNanos);
  }

  private static final class PropagatingConsumer<X> implements Consumer<X>, Wrapper<Consumer<X>> {

    private final Consumer<X> function;
    private final ExecutionContext current;

    private final String name;

    private final long deadlineNanos;

    PropagatingConsumer(final Consumer<X> task, final ExecutionContext current,
            final String name, final long deadlineNanos) {
      this.function = task;
      this.current = current;
      this.name = name;
      this.deadlineNanos = deadlineNanos;
    }

    @Override
    public void accept(final X in) {
      try (ExecutionContext ctx = start(name, current, deadlineNanos)) {
        function.accept(in);
      }
    }

    @Override
    public String toString() {
      return name == null ? function.toString() : name;
    }

    @Override
    public Consumer<X> getWrapped() {
      return function;
    }

  }

  public static <X, Y> BiConsumer<X, Y> propagatingBiConsumer(final BiConsumer<X, Y> callable,
          final ExecutionContext ctx,
          final String name, final long deadlineNanos) {
    return new PropagatingBiConsumer<>(callable, ctx, name, deadlineNanos);
  }

  private static final class PropagatingBiConsumer<X, Y> implements BiConsumer<X, Y>, Wrapper<BiConsumer<X, Y>> {

    private final BiConsumer<X, Y> function;
    private final ExecutionContext current;

    private final String name;

    private final long deadlineNanos;

    PropagatingBiConsumer(final BiConsumer<X, Y> task, final ExecutionContext current,
            final String name, final long deadlineNanos) {
      this.function = task;
      this.current = current;
      this.name = name;
      this.deadlineNanos = deadlineNanos;
    }

    @Override
    public void accept(final X x, final Y y) {
      try (ExecutionContext ctx = start(name, current, deadlineNanos)) {
        function.accept(x, y);
      }
    }

    @Override
    public String toString() {
      return name == null ? function.toString() : name;
    }

    @Override
    public BiConsumer<X, Y> getWrapped() {
      return function;
    }

  }



  private static final class PropagatingRunnable implements Runnable, Wrapper<Runnable> {

    private final Runnable task;
    private final ExecutionContext current;
    private final String name;
    private final long deadlineNanos;

    PropagatingRunnable(final Runnable task, final ExecutionContext current, final String name,
            final long deadlineNanos) {
      this.task = task;
      this.current = current;
      this.name = name;
      this.deadlineNanos = deadlineNanos;
    }

    @Override
    public void run() {
      try (ExecutionContext ctx = start(name, current, deadlineNanos)) {
        task.run();
      }
    }

    @Override
    public Runnable getWrapped() {
      return task;
    }

    @Override
    public String toString() {
     return name == null ? task.toString() : name;
    }
  }

  private static class ThreadLocalScopeImpl implements ThreadLocalContextAttacher {

    @Override
    public Attached attach(final ExecutionContext ctx) {
      final Thread currentThread = Thread.currentThread();
      SimpleStack<ExecutionContext> contextStack = ExecutionContexts.EXEC_CTX.get();
      int stackPtr = contextStack.pushAndGetIdx(ctx);
      return new AttachedImpl(currentThread, contextStack, ctx, stackPtr);
    }

    private static class AttachedImpl implements Attached {

      private final Thread thread;
      private final SimpleStack<ExecutionContext> contextStack;
      private final ExecutionContext ctx;
      private final int stackPtr;

      AttachedImpl(final Thread currentThread,
              final SimpleStack<ExecutionContext> contextStack,
              final ExecutionContext ctx, final int stackPtr) {
        this.thread = currentThread;
        this.contextStack = contextStack;
        this.ctx = ctx;
        this.stackPtr = stackPtr;
      }

      @Override
      public void detach() {
        Thread now = Thread.currentThread();
        if (now != thread) {
          throw  new IllegalStateException("Detaching in different thread " + thread + " != " + now);
        }
        ExecutionContext pop = contextStack.pop();
        if (pop != ctx) {
          contextStack.push(pop);
          throw  new IllegalStateException("Detaching ctx that is not attached " + ctx);
        }
      }

      public boolean isTopOfStack() {
        return stackPtr == 0;
      }

      public Thread attachedThread() {
        return thread;
      }

    }
  }

}
