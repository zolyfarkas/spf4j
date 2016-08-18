/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.stackmonitor;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spf4j.base.ArrayBuilder;

@Beta
public final class ObservableOnlyStackCollector extends AbstractStackCollector {

  private final ConcurrentMap<Thread, Consumer<StackTraceElement[]>> stConsumers;


  public ObservableOnlyStackCollector() {
    this.stConsumers = new ConcurrentHashMap<>();
  }

  @Nullable
  public Consumer<StackTraceElement[]> registerConsumer(final Thread thread,
          final Consumer<StackTraceElement[]> consumer) {
    return stConsumers.put(thread, consumer);
  }

  public Consumer<StackTraceElement[]> unregisterConsumer(final Thread thread) {
    return stConsumers.remove(thread);
  }

  private ArrayBuilder<Thread> requestFor = new ArrayBuilder<>(8, Thread.class);

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void sample(final Thread ignore) {
    requestFor.clear();
    for (Thread t : stConsumers.keySet()) {
      requestFor.add(t);
    }
    Thread[] rfArray = requestFor.getArray();
    StackTraceElement[][] stackDump = FastStackCollector.getStackTraces(rfArray);
    int size = requestFor.getSize();
    for (int i = 0; i < size; i++) {
      Thread t = rfArray[i];
      StackTraceElement[] stackTrace = stackDump[i];
      Consumer<StackTraceElement[]> consumer = this.stConsumers.get(t);
      if (consumer != null) {
        consumer.accept(stackTrace);
      }
      if (stackTrace != null && stackTrace.length > 0) {
        addSample(stackTrace);
      } else {
        addSample(new StackTraceElement[]{
          new StackTraceElement("Thread", t.getName(), "", 0)
        });
      }
    }
  }

}
