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
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.spf4j.concurrent.DefaultScheduler;

/**
 * A Utility class that allows for quick conversion between nanotime and epoch relative time.
 * @author Zoltan Farkas
 */
@Beta
public final class Timing {

  public static final long MAX_MS_SPAN = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE);

  private static final long TIMING_UPDATE_INTERVAL_MINUTES = Long.getLong("spf4j.timing.updateIntervalMinutes", 60);

  private static volatile Timing latestTiming;

  private static final ScheduledFuture UPDATER;

  static {
    latestTiming = new Timing();
    UPDATER = DefaultScheduler.INSTANCE.scheduleWithFixedDelay(() -> latestTiming = new Timing(),
            TIMING_UPDATE_INTERVAL_MINUTES, TIMING_UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES);
  }

  private final long nanoTimeRef;
  private final long currentTimeMillisRef;

  private Timing() {
    nanoTimeRef = TimeSource.nanoTime();
    currentTimeMillisRef = System.currentTimeMillis();
  }

  public long fromNanoTimeToEpochMillis(final long nanoTime) {
    return currentTimeMillisRef + TimeUnit.NANOSECONDS.toMillis(nanoTime - nanoTimeRef);
  }

  public Instant fromNanoTimeToInstant(final long nanoTime) {
    return Instant.ofEpochMilli(currentTimeMillisRef + TimeUnit.NANOSECONDS.toMillis(nanoTime - nanoTimeRef));
  }

  public long fromEpochMillisToNanoTime(final long epochTimeMillis) {
    long msSinceLast = epochTimeMillis - currentTimeMillisRef;
    if (Math.abs(msSinceLast) > MAX_MS_SPAN) {
      return TimeSource.nanoTime() + Long.MAX_VALUE;
//      throw new IllegalArgumentException("Epoch time millis cannot be converted to nanotime " + epochTimeMillis);
    }
    return nanoTimeRef + TimeUnit.MILLISECONDS.toNanos(msSinceLast);
  }

  public static Timing getCurrentTiming() {
    return latestTiming;
  }


  public static void stopUpdate() {
    UPDATER.cancel(false);
  }

  @Override
  public String toString() {
    return "Timing{" + "nanoTimeRef=" + nanoTimeRef + ", currentTimeMillisRef=" + currentTimeMillisRef + '}';
  }


}
