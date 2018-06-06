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
package org.spf4j.perf.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class MemoryUsageSamplerTest {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryUsageSamplerTest.class);

  @Test
  public void testMemoryUsageSampler() throws InterruptedException, IOException {
    System.setProperty("perf.memory.sampleAggMillis", "1000");
    assertSamplerBehavior();
    assertSamplerBehavior();
  }

  @SuppressFBWarnings("MDM_THREAD_YIELD")
  private void assertSamplerBehavior() throws InterruptedException, IOException {
    MemoryUsageSampler.start(100);
    String str = "";
    for (int i = 0; i < 100000; i++) {
      str = Integer.toString(i);
    }
    LOG.debug("lastNr = {}", str);
    Thread.sleep(1000);
    MemoryUsageSampler.stop();
    Assert.assertFalse(MemoryUsageSampler.isStarted());
  }

}
