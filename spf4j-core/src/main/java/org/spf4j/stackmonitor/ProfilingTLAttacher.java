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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.ThreadLocalContextAttacher;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class ProfilingTLAttacher implements ThreadLocalContextAttacher {

  private final ConcurrentMap<Thread, ExecutionContext> currentContexts;

  public ProfilingTLAttacher() {
    this.currentContexts =
            Boolean.getBoolean("spf4j.ctxtProfiler.regDs.skipList") ?
            new ConcurrentSkipListMap<>(ProfilingTLAttacher::compare)
            : new ConcurrentHashMap<>(Integer.getInteger("spf4j.ctxtProfiler.regDs.concMap.initialSize", 64),
                    Float.parseFloat(System.getProperty("spf4j.ctxtProfiler.regDs.concMap.loadFactor", "0.8")),
                    Integer.getInteger("spf4j.ctxtProfiler.regDs.concMap.concurrencyLevel", 32));
  }

  private static int compare(final Thread o1, final Thread o2) {
    return Long.compare(o1.getId(), o2.getId());
  }

  public Iterable<Thread> getCurrentThreads() {
    return currentContexts.keySet();
  }

  public Iterable<Map.Entry<Thread, ExecutionContext>> getCurrentThreadContexts() {
    return currentContexts.entrySet();
  }

  @Override
  public ThreadLocalContextAttacher.Attached attach(final ExecutionContext ctx) {
    ThreadLocalContextAttacher.Attached attached = ExecutionContexts.defaultThreadLocalAttacher().attach(ctx);
    if (attached.isTopOfStack()) {
      currentContexts.put(attached.attachedThread(), ctx);
      return new ThreadLocalContextAttacher.Attached() {
        @Override
        public void detach() {
          currentContexts.remove(attached.attachedThread());
          attached.detach();
        }

        @Override
        public boolean isTopOfStack() {
          return attached.isTopOfStack();
        }

        @Override
        public Thread attachedThread() {
          return attached.attachedThread();
        }
      };
    } else {
      return attached;
    }
  }

  @Override
  public String toString() {
    return "ProfilingTLAttacher{" + "currentContexts=" + currentContexts + '}';
  }

}
