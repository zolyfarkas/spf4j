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
package org.spf4j.stackmonitor;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.spf4j.base.ExecutionContext;

public final class ThreadSpecificTracingExecutionContextHandler extends TracingExecutionContexSampler {

  public ThreadSpecificTracingExecutionContextHandler(
          final Supplier<Iterable<Map.Entry<Thread, ExecutionContext>>> execCtxSupplier,
          final Function<ExecutionContext, String> ctxToCategory) {
    super(execCtxSupplier, ctxToCategory);
  }

  public ThreadSpecificTracingExecutionContextHandler(final int maxNrThreads,
          final Supplier<Iterable<Map.Entry<Thread, ExecutionContext>>> execCtxSupplier,
          final Function<ExecutionContext, String> ctxToCategory) {
    super(maxNrThreads, execCtxSupplier, ctxToCategory);
  }

  @Override
  protected int prepareThreadsAndContexts(final Iterable<Map.Entry<Thread, ExecutionContext>> currentThreads) {
    int i = 0;
    for (Map.Entry<Thread, ExecutionContext> entry : currentThreads) {
      requestFor[i] = entry.getKey();
      contexts[i++] = entry.getValue();
      if (i >= requestFor.length) {
        break;
      }
    }
    return i;
  }


}
