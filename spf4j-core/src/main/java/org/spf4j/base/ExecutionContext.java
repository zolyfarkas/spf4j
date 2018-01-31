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
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author Zoltan Farkas
 */
@CleanupObligation
@ParametersAreNonnullByDefault
public interface ExecutionContext extends AutoCloseable {

  @DischargesObligation
  void close();

  long getDeadlineNanos();

  ExecutionContext getParent();

  default long getUnitsToDeadline(final TimeUnit unit) {
     return unit.convert(getDeadlineNanos() - TimeSource.nanoTime(), TimeUnit.NANOSECONDS);
  }


  /**
   * Method to get baggage.
   * @param <T> type of baggage.
   * @param clasz class of baggage.
   * @return the baggage
   */
  @Nullable
  @Beta
  <T> T get(Class<T> clasz);


  /**
   * Method to put baggage.
   * @param <T> type of baggage.
   * @param data the baggage.
   * @return existing baggage if there.
   */
  @Nullable
  @Beta
  <T> T put(T data);


}
