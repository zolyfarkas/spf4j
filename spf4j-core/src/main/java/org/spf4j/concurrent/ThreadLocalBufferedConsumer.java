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
package org.spf4j.concurrent;

import com.google.common.annotations.Beta;
import java.io.Closeable;
import java.io.Flushable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 *
 * @author zoly
 */
@Beta
public final class ThreadLocalBufferedConsumer<T> implements Flushable, Closeable {

  private final ThreadLocal<List<T>> localBuffer;

  private final Map<Thread, List<T>> buffers;

  private final Consumer<List<T>> consumer;

  private final int localSize;

  private final ScheduledFuture<?> schedule;

  public ThreadLocalBufferedConsumer(final int localSize, final Consumer<List<T>> consumer, final int delayMillis) {
    this.localSize = localSize;
    buffers = new ConcurrentHashMap<>();
    this.consumer = consumer;
    localBuffer = new ThreadLocal<List<T>>() {
      @Override
      protected List<T> initialValue() {
        List<T> result = new ArrayList<>(localSize);
        buffers.put(Thread.currentThread(), result);
        return result;
      }
    };
    this.schedule = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(this::flush,
            delayMillis, delayMillis, TimeUnit.MILLISECONDS);
  }

  public void write(final T value) {
    List<T> lb = localBuffer.get();
    synchronized (lb) {
      if (lb.size() >= localSize) {
        this.consumer.accept(lb);
        lb.clear();
      }
      lb.add(value);
    }
  }

  @Override
  public void flush() {
    Iterator<Map.Entry<Thread, List<T>>> iterator = buffers.entrySet().iterator();
    List<T> toWrite = new ArrayList<>(localSize);
    while (iterator.hasNext()) {
      Map.Entry<Thread, List<T>> entry = iterator.next();
      Thread thread = entry.getKey();
      if (!thread.isAlive()) {
        iterator.remove();
      }
      List<T> lb = entry.getValue();
      synchronized (lb) {
        if (!lb.isEmpty()) {
          toWrite.addAll(lb);
        }
        lb.clear();
      }
    }
    this.consumer.accept(toWrite);
  }

  @Override
  public synchronized void close() {
    if (!schedule.isCancelled()) {
      schedule.cancel(false);
      flush();
    }
  }

  @Override
  public String toString() {
    return "ThreadLocalBufferedConsumer{ consumer=" + consumer + ", localSize="
            + localSize + ", schedule=" + schedule + '}';
  }

}
