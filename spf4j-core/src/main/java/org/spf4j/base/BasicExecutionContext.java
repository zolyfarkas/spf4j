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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import org.codehaus.jackson.JsonGenerator;
import org.spf4j.io.AppendableWriter;
import org.spf4j.log.Level;
import org.spf4j.log.Slf4jLogRecord;
import org.spf4j.base.ThreadLocalContextAttacher.Attached;

/**
 * The simplest execution context possible.
 *
 * @author Zoltan Farkas
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public class BasicExecutionContext implements ExecutionContext {

  private static final int MX_NR_LOGS_PER_CTXT = Integer.getInteger("spf4j.execContext.maxNrLogsPerContext", 100);

  private static final Level MIN_LOG_LEVEL
          = Level.valueOf(System.getProperty("spf4j.execContext.minLogLevel", "TRACE"));

  private final String name;

  private final CharSequence id;

  private final ExecutionContext parent;

  private final Relation relation;

  private final long startTimeNanos;

  private final long deadlineNanos;

  private ArrayDeque<Slf4jLogRecord> logs;

  private Map<Tag, Object> baggage;

  private long childCount;

  private boolean isClosed = false;

  private Level minBackendLogLevel;

  private volatile Attached attached;

  @SuppressWarnings("unchecked")
  @SuppressFBWarnings("STT_TOSTRING_STORED_IN_FIELD")
  public BasicExecutionContext(final String name, @Nullable final CharSequence id,
          @Nullable final ExecutionContext parent, final Relation relation,
          final long startTimeNanos, final long deadlineNanos) {
    this.isClosed = false;
    this.relation = relation;
    this.name = name;
    this.startTimeNanos = startTimeNanos;
    if (parent != null) {
      long parentDeadline = parent.getDeadlineNanos();
      if (parentDeadline < deadlineNanos) {
        this.deadlineNanos = parentDeadline;
      } else {
        this.deadlineNanos = deadlineNanos;
      }
      if (id == null) {
        CharSequence pId = parent.getId();
        StringBuilder sb = new StringBuilder(pId.length() + 2).append(pId).append('/');
        AppendableUtils.appendUnsignedString(sb, parent.nextChildId(), 5);
        this.id  = sb;
      } else {
        this.id  = id;
      }
    } else {
      this.deadlineNanos = deadlineNanos;
      this.id  = id == null ? ExecutionContexts.genId() : id;
    }
    this.parent = parent;
    this.baggage = Collections.EMPTY_MAP;
    this.logs = null;
    this.minBackendLogLevel = null;
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final long getDeadlineNanos() {
    return deadlineNanos;
  }

  public final long getStartTimeNanos() {
    return startTimeNanos;
  }

  @Nullable
  @Beta
  @Override
  public final synchronized <T> T put(@Nonnull final Tag<T> key, @Nonnull final T data) {
    if (baggage == Collections.EMPTY_MAP) {
      baggage = new HashMap<>(4);
    }
    return (T) baggage.put(key, data);
  }

  @Nullable
  @Beta
  @Override
  public final synchronized <T> T get(@Nonnull final Tag<T> key) {
    Object res = baggage.get(key);
    if (res == null) {
      if (parent != null) {
        return parent.get(key);
      } else {
        return null;
      }
    } else {
      return (T) res;
    }
  }

  @Override
  @Nullable
  public final synchronized <V> V compute(@Nonnull final Tag<V> key, final BiFunction<Tag<V>, V, V> compute) {
    if (baggage == Collections.EMPTY_MAP) {
      baggage = new HashMap(4);
    }
    return (V) baggage.compute(key, (BiFunction) compute);
  }

  @Override
  public final ExecutionContext getParent() {
    return parent;
  }

  /**
   * Close might be overridable to close any additional stuff added in the extended class.
   */
  @Override
  public synchronized void close() {
    if (!isClosed) {
      detach();
      if (relation == Relation.CHILD_OF) {
        for (Slf4jLogRecord log : logs) {
          parent.addLog(log);
        }
      }
      isClosed = true;
    }
  }

  @Override
  public final void detach() {
    attached.detach();
  }

  @Override
  public final void attach() {
    attached = ExecutionContexts.threadLocalAttacher().attach(this);
  }

  /**
   * Overwrite as needed for debug string.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(64);
    writeTo(sb);
    return sb.toString();
  }

  /**
   * Overwrite this method to change default json format.
   * @param appendable
   */
  @Override
  public synchronized void writeJsonTo(final Appendable appendable) throws IOException {
    JsonGenerator gen = Json.FACTORY.createJsonGenerator(new AppendableWriter(appendable));
    gen.setCodec(Json.MAPPER);
    gen.writeStartObject();
    gen.writeFieldName("name");
    gen.writeString(name);
    gen.writeFieldName("startTs");
    Timing currentTiming = Timing.getCurrentTiming();
    gen.writeString(currentTiming.fromNanoTimeToInstant(startTimeNanos).toString());
    gen.writeFieldName("deadlineTs");
    gen.writeString(currentTiming.fromNanoTimeToInstant(deadlineNanos).toString());
    for (Map.Entry<Tag, Object> entry : baggage.entrySet()) {
      gen.writeFieldName(entry.getKey().toString());
      gen.writeObject(entry.getValue());
    }
    gen.writeEndObject();
    gen.flush();
  }

  @Override
  public final synchronized void addLog(final Slf4jLogRecord log) {
    if (logs == null) {
      logs = new ArrayDeque<>(4);
    }
    if (logs.size() >= MX_NR_LOGS_PER_CTXT) {
      logs.removeFirst();
      logs.addLast(log);
    } else {
      logs.addLast(log);
    }
  }

  @Override
  public final synchronized void streamLogs(final Consumer<Slf4jLogRecord> to) {
    if (logs != null) {
      for (Slf4jLogRecord log : logs) {
        to.accept(log);
      }
    }
  }

  /**
   * Overwrite for more configurable implementation.
   * @param loggerName
   * @return
   */
  @Override
  public Level getContextMinLogLevel(final String loggerName) {
    return MIN_LOG_LEVEL;
  }

  /**
   * Overwrite for more configurable implementation.
   * @param loggerName
   * @return
   */
  @Override
  public synchronized Level getBackendMinLogLevel(final String loggerName) {
    return minBackendLogLevel;
  }

  /**
   * Overwrite for more configurable implementation.
   * @param loggerName
   * @return
   */
  @Override
  public Level setBackendMinLogLevel(final String loggerName, final Level level) {
    Level result = minBackendLogLevel;
    minBackendLogLevel = level;
    return result;
  }

  @Override
  public final CharSequence getId() {
    return id;
  }

  @Override
  public final synchronized long nextChildId() {
    return childCount++;
  }


}
