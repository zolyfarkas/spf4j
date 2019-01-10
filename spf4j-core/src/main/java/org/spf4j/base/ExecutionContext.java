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

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Signed;
import org.spf4j.log.Level;
import org.spf4j.log.Slf4jLogRecord;

/**
 * Execution context information encapsulated a place to store execution related information:
 * <ul>
 * <li>deadline/timeout</li>
 * <li>context logs/overrides</li>
 * <li>tagged attachments (profiling info, etc..)</li>
 * </ul>
 * @author Zoltan Farkas
 */
@CleanupObligation
@ParametersAreNonnullByDefault
public interface ExecutionContext extends AutoCloseable, JsonWriteable {

  public interface Tag<T> {

    String toString();
    
  }

  enum Relation {
    CHILD_OF, FOLLOWS
  }

  @DischargesObligation
  void close();

  @Nonnull
  String getName();

  CharSequence getId();

  long getStartTimeNanos();

  long getDeadlineNanos();

  @Nullable
  ExecutionContext getSource();

  /**
   * @return the top source. will return this is current is root.
   * will follow all relationship types.
   */
  default ExecutionContext getRoot() {
    ExecutionContext curr = this;
    ExecutionContext parent;
    while ((parent = curr.getSource()) != null) {
      curr = parent;
    }
    return curr;
  }

  /**
   * @return Adam of this current context. will return this is current is Adam.
   * follows only CHILD_OF relationships.
   */
  default ExecutionContext getRootParent() {
    ExecutionContext curr = this;
    ExecutionContext parent;
    while (curr.getRelationToSource() == Relation.CHILD_OF && (parent = curr.getSource()) != null) {
      curr = parent;
    }
    return curr;
  }

  /**
   * @return will return the first not closed parent. null if no parent is available.
   */
  @Nullable
  default ExecutionContext getNotClosedParent() {
    ExecutionContext curr = this;
    ExecutionContext parent;
    while (curr.getRelationToSource() == Relation.CHILD_OF
            && (parent = curr.getSource()) != null && parent.isClosed()) {
      curr = parent;
    }
    return curr == this ? null : curr;
  }

  @Beta
  void addLog(Slf4jLogRecord log);

  @Beta
  void addLogs(Collection<Slf4jLogRecord> log);

  /**
   * Attach a AutoCloseable to execution context.
   * All of them will be closed when context is closed, in reverse registration order.
   * @param closeable
   */
  @Beta
  void addCloseable(AutoCloseable closeable);

  /**
   * The minimum log level accepted by this execution context;
   * The logs that we will store in this context.
   * @return
   */
  @Beta
  Level getContextMinLogLevel(String loggerName);

  /**
   * The minimum log level overwrite.
   * An execution context can overwrite the backend configured log level.
   * @return null if not specified.
   */
  @Beta
  @Nullable
  Level getBackendMinLogLevel(String loggerName);

  @Beta
  @Nullable
  Level setBackendMinLogLevel(String loggerName, Level level);

  @Beta
  void streamLogs(Consumer<Slf4jLogRecord> to);

  void detach();

  void attach();


  @Nonnegative
  default long getTimeToDeadline(final TimeUnit unit) throws TimeoutException {
     long result = getTimeRelativeToDeadline(unit);
     if (result <= 0) {
       throw new TimeoutException("Deadline exceeded by " + (-result) + ' ' + unit);
     }
     return result;
  }

  @Nonnegative
  default long getUncheckedTimeToDeadline(final TimeUnit unit) {
     long result = getTimeRelativeToDeadline(unit);
     if (result <= 0) {
       throw new UncheckedTimeoutException("Deadline exceeded by " + (-result) + ' ' + unit);
     }
     return result;
  }

  @Signed
  default long getTimeRelativeToDeadline(final TimeUnit unit)  {
     return unit.convert(getDeadlineNanos() - TimeSource.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Nonnegative
  default long getMillisToDeadline() throws TimeoutException {
    return getTimeToDeadline(TimeUnit.MILLISECONDS);
  }

  @Nonnegative
  default int getSecondsToDeadline() throws TimeoutException {
    long secondsToDeadline = getTimeToDeadline(TimeUnit.SECONDS);
    if (secondsToDeadline > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) secondsToDeadline;
    }
  }

  /**
   * Method to get context associated data.
   * if current context does not have baggage, the parent context is queried.
   * @param <T> type of data.
   * @param key key of data.
   * @return the data
   */
  @Nullable
  @Beta
  <T> T get(Tag<T> key);


  /**
   * Method to put context associated data.
   * @param <T> type of data.
   * @param key the key of data.
   * @param data the data.
   * @return existing data if there.
   */
  @Nullable
  @Beta
  <T> T put(Tag<T> tag, T data);

  /**
   * Method to put context associated data to the root context.
   * @param <T> type of data.
   * @param key the key of data.
   * @param data the data.
   * @return existing data if there.
   */
  @Nullable
  @Beta
  default <T> T putToRootParent(final Tag<T> key, final T data) {
    return getRootParent().put(key, data);
  }

  /**
   * Compute context associated data.
   * @param <K>
   * @param <V>
   * @param key
   * @param compute
   * @return
   */
  @Beta
  @Nullable
  <V> V compute(Tag<V> key, BiFunction<Tag<V>, V, V> compute);


  @Nullable
  @Beta
  default <T> List<T> add(final Tag<List<T>> tag, final T data) {
    return compute(tag, (k, v) -> {
      if (v == null)  {
        v = new ArrayList<>(2);
      }
      v.add(data);
      return v;
    });
  }

  @Nullable
  @Beta
  default <T> List<T> addToRootParent(final Tag<List<T>> tag, final T data) {
    ExecutionContext ctx = getRootParent();
    return ctx.compute(tag, (k, v) -> {
      if (v == null)  {
        v = new ArrayList<>(2);
      }
      v.add(data);
      return v;
    });
  }

  default ExecutionContext startChild(final String operationName,
          final long timeout, final TimeUnit tu) {
    return ExecutionContexts.start(operationName, this, timeout, tu);
  }

  default ExecutionContext startChild(final String operationName) {
    return ExecutionContexts.start(operationName, this);
  }

  default ExecutionContext detachedChild(final String operationName,
          final long timeout, final TimeUnit tu) {
    return ExecutionContexts.createDetached(operationName, this, timeout, tu);
  }

  default ExecutionContext detachedChild(final String operationName) {
    return ExecutionContexts.createDetached(operationName, this, TimeSource.nanoTime(), this.getDeadlineNanos());
  }

  long nextChildId();

  void add(StackTraceElement[] sample);

  void add(StackSamples samples);

  @Nullable
  StackSamples getAndClearStackSamples();

  @Nullable
  StackSamples getStackSamples();


  boolean isClosed();

  Relation getRelationToSource();

}
