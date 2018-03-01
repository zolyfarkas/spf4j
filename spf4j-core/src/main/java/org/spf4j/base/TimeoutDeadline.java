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

import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnegative;

/**
 * @author Zoltan Farkas
 */
public final class TimeoutDeadline {

  private final long timeoutNanos;

  private final long deadlineNanos;

  public TimeoutDeadline(@Nonnegative final long timeoutNanos, final long deadlineNanos) {
    this.timeoutNanos = timeoutNanos;
    this.deadlineNanos = deadlineNanos;
  }

  public static TimeoutDeadline of(final long timeoutNanos, final long deadlineNanos)
    throws TimeoutException {
    if (timeoutNanos < 0) {
      throw new TimeoutException("Timeout exceeded by " + (-timeoutNanos) + " ns, deadline: "
              + Timing.getCurrentTiming().fromNanoTimeToInstant(deadlineNanos));
    }
    return new TimeoutDeadline(timeoutNanos, deadlineNanos);
  }


  public long getTimeoutNanos() {
    return timeoutNanos;
  }

  public long getDeadlineNanos() {
    return deadlineNanos;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 23 * hash + (int) (this.timeoutNanos ^ (this.timeoutNanos >>> 32));
    return 23 * hash + (int) (this.deadlineNanos ^ (this.deadlineNanos >>> 32));
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
    final TimeoutDeadline other = (TimeoutDeadline) obj;
    if (this.timeoutNanos != other.timeoutNanos) {
      return false;
    }
    return this.deadlineNanos == other.deadlineNanos;
  }

  @Override
  public String toString() {
    return "TimeoutDeadline{" + "timeoutNanos=" + timeoutNanos + ", deadlineNanos=" + deadlineNanos + '}';
  }


}
