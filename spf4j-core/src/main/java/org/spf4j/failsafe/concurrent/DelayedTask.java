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
package org.spf4j.failsafe.concurrent;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.TimeSource;

/**
 *
 * @author Zoltan Farkas
 */
class DelayedTask<R extends Runnable> implements Delayed {

  private final R runnable;
  private final long deadlineNanos;

  DelayedTask(final R runnable, final long delayNanos) {
    this.runnable = runnable;
    this.deadlineNanos = TimeSource.getDeadlineNanos(delayNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public long getDelay(final TimeUnit unit) {
    return TimeSource.getTimeToDeadline(deadlineNanos, unit);
  }

  @Override
  public int compareTo(final Delayed o) {
    long tDelay = getDelay(TimeUnit.NANOSECONDS);
    long oDelay = o.getDelay(TimeUnit.NANOSECONDS);
    if (tDelay > oDelay) {
      return 1;
    } else if (tDelay < oDelay) {
      return -1;
    } else {
      return 0;
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final DelayedTask<?> other = (DelayedTask<?>) obj;
    if (this.deadlineNanos != other.deadlineNanos) {
      return false;
    }
    return Objects.equals(this.runnable, other.runnable);
  }

  @Override
  public int hashCode() {
    return this.runnable.hashCode();
  }

  public R getRunnable() {
    return runnable;
  }

  @Override
  public String toString() {
    return "DelayedTask{" + "runnable=" + runnable + ", deadlineNanos=" + deadlineNanos + '}';
  }

}
