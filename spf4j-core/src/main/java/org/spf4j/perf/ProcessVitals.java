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

import org.spf4j.perf.cpu.CpuUsageSampler;
import org.spf4j.perf.cpu.ThreadUsageSampler;
import org.spf4j.perf.io.OpenFilesSampler;
import org.spf4j.perf.memory.GCUsageSampler;
import org.spf4j.perf.memory.MemoryUsageSampler;

/**
 * Utility monitoring to collect
 * @author Zoltan Farkas
 */
public final class ProcessVitals implements AutoCloseable {

  private final int openFilesSampleTimeMillis;

  private final int memoryUseSampleTimeMillis;

  private final int gcUseSampleTimeMillis;

  private final int threadUseSampleTimeMillis;

  private final int cpuUseSampleTimeMillis;

  public ProcessVitals() {
    this(Integer.getInteger("spf4j.vitals.openFilesSampleTimeMillis", 60000),
         Integer.getInteger("spf4j.vitals.memoryUseSampleTimeMillis", 6000),
         Integer.getInteger("spf4j.vitals.gcUseSampleTimeMillis", 10000),
         Integer.getInteger("spf4j.vitals.threadUseSampleTimeMillis", 10000),
         Integer.getInteger("spf4j.vitals.cpuUseSampleTimeMillis", 10000));
  }


  public ProcessVitals(final int openFilesSampleTimeMillis,
          final int memoryUseSampleTimeMillis,
          final int gcUseSampleTimeMillis,
          final int threadUseSampleTimeMillis,
          final int cpuUseSampleTimeMillis) {
    this.openFilesSampleTimeMillis = openFilesSampleTimeMillis;
    this.memoryUseSampleTimeMillis = memoryUseSampleTimeMillis;
    this.gcUseSampleTimeMillis = gcUseSampleTimeMillis;
    this.threadUseSampleTimeMillis = threadUseSampleTimeMillis;
    this.cpuUseSampleTimeMillis = cpuUseSampleTimeMillis;
  }




  public void start() {
    OpenFilesSampler.start(openFilesSampleTimeMillis);
    MemoryUsageSampler.start(memoryUseSampleTimeMillis);
    GCUsageSampler.start(gcUseSampleTimeMillis);
    ThreadUsageSampler.start(threadUseSampleTimeMillis);
    CpuUsageSampler.start(cpuUseSampleTimeMillis);
  }


  @Override
  public void close()  {
    OpenFilesSampler.stop();
    MemoryUsageSampler.stop();
    GCUsageSampler.stop();
    ThreadUsageSampler.stop();
    CpuUsageSampler.stop();
  }

  @Override
  public String toString() {
    return "ProcessVitals{" + "openFilesSampleTimeMillis=" + openFilesSampleTimeMillis
            + ", memoryUseSampleTimeMillis=" + memoryUseSampleTimeMillis + ", gcUseSampleTimeMillis="
            + gcUseSampleTimeMillis + ", threadUseSampleTimeMillis=" + threadUseSampleTimeMillis
            + ", cpuUseSampleTimeMillis=" + cpuUseSampleTimeMillis + '}';
  }




}
