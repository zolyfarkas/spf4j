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
import gnu.trove.map.hash.THashMap;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
final class BasicExecutionContext implements ExecutionContext {
  /**
   * The previous context on Thread.
   */
  private final ExecutionContext tParent;

  private final ExecutionContext parent;

  private final long deadlineNanos;

  private Map<Class, Object> baggage;


  @SuppressWarnings("unchecked")
  BasicExecutionContext(final ExecutionContext parent, final ExecutionContext tParent,
          final long deadlineNanos) {
    this.deadlineNanos = deadlineNanos;
    this.tParent = tParent;
    this.parent = parent;
    this.baggage = Collections.EMPTY_MAP;
  }

  @Override
  public long getDeadlineNanos() {
    return deadlineNanos;
  }

  @Override
  public ExecutionContext subCtx(final long pdeadlineNanos) {
    ExecutionContext xCtx = ExecutionContexts.current();
    ExecutionContext ctx = new BasicExecutionContext(this, xCtx, pdeadlineNanos);
    ExecutionContexts.setCurrent(ctx);
    return ctx;
  }


  @Nullable
  @Beta
  @Override
  public synchronized <T> T put(@Nonnull final T data) {
    if (baggage.isEmpty()) {
      baggage = new THashMap<>(2);
    }
    return (T) baggage.put(data.getClass(), data);
  }

  @Nullable
  @Beta
  @Override
  public synchronized <T> T get(@Nonnull final Class<T> clasz) {
    T res = (T) baggage.get(clasz);
    if (res == null) {
      if (parent != null) {
        return parent.get(clasz);
      } else {
        return null;
      }
    } else {
      return res;
    }
  }

  @Override
  public ExecutionContext getParent() {
    return parent;
  }

  @Override
  public void close()  {
    ExecutionContexts.setCurrent(this.tParent);
  }

  @Override
  public String toString() {
    return "BasicExecutionContext{" + "tParent=" + tParent + ", parent=" + parent
            + ", deadlineNanos=" + deadlineNanos + ", baggage=" + baggage + '}';
  }

}
