/*
 * Copyright 2018 SPF4J.
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
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TLongArrayList;
import java.util.function.LongSupplier;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zoltan Farkas
 */
@Beta
public final class TestTimeSource implements LongSupplier {

  private static final Logger LOG = LoggerFactory.getLogger(TestTimeSource.class);

  private static final TLongIterator EMPTY_STREAM = new TLongArrayList(0).iterator();

  private static volatile TLongIterator timeStream = EMPTY_STREAM;

  public static void clear() {
    timeStream = EMPTY_STREAM;
  }

  public static void addTimeStream(final long... times) {
    timeStream = new TLongArrayList(times).iterator();
  }

  @Override
  @Nonnull
  public long getAsLong() {
    long nextTime;
    if (timeStream.hasNext()) {
      nextTime = timeStream.next();
    } else {
      nextTime = System.nanoTime();
    }
    if (LOG.isTraceEnabled()) {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      LOG.trace("nanoTime = {} at {}", nextTime, stackTrace.length > 2 ? stackTrace[2] : "unknown");
    }
    return nextTime;
  }

}
