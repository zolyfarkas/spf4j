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
package org.spf4j.perf;

import java.io.Closeable;
import java.io.IOException;

/**
 * A measurement store.
 *
 * @author zoly
 */
public interface MeasurementStore extends Closeable {

  /**
   * Make any allocations necessary for the following measurements.
   *
   * @param measurementInfo - the information about the measurement(s)
   * @param sampleTimeMillis - the expected sample time. (interval between the stored measurements).
   * @return - the id of the measurementInfo table.
   * @throws IOException - IO issues.
   */
  long alocateMeasurements(MeasurementsInfo measurementInfo, int sampleTimeMillis)
          throws IOException;

  /**
   * Save measurements.
   *
   * @param tableId - the table ID to store measurements for.
   * @param timeStampMillis - the timestamp of the measurement (millis since Jan 1 1970 UTC)
   * @param measurements - the measurements to persist. (same order as declared)
   * @throws IOException - IO issues.
   */
  void saveMeasurements(long tableId, long timeStampMillis, long... measurements)
          throws IOException;

  /**
   * flush all data that might be buffered by this store.
   *
   * @throws IOException - IO issues.
   */
  void flush() throws IOException;

}
