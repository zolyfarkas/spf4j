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
package org.spf4j.stackmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.ssdump2.Converter;

@NotThreadSafe
public final class DemoTest {

  private static final Logger LOG = LoggerFactory.getLogger(DemoTest.class);

  private static volatile boolean stopped;

  @Test
  public void testJmx() throws InterruptedException, IOException {
    Sampler sampler = new Sampler((t) -> new SimpleStackCollector(t));
    sampler.registerJmx();
    sampler.start();
    main(new String[]{});
    sampler.stop();
    SampleNode original = sampler.getStackCollections().values().iterator().next();
    File file = sampler.dumpToFile();
    LOG.debug("Samples saved to {}", file);
    Assert.assertNotNull(file);
    Assert.assertTrue(file.exists());
    SampleNode loaded = Converter.load(file);
    Assert.assertEquals(original, loaded);
    sampler.stop();
  }

  @SuppressFBWarnings("MDM_THREAD_YIELD")
  public static void main(final String[] args) throws InterruptedException {
    List<Thread> threads = startTestThreads(20);
    Thread.sleep(5000);
    stopTestThreads(threads);
  }

  public static void stopTestThreads(final List<Thread> threads) throws InterruptedException {
    stopped = true;
    for (Thread t : threads) {
      t.join(3000);
    }
  }

  public static List<Thread> startTestThreads(final int nrThreads) {
    stopped = false;
    List<Thread> threads = new ArrayList<>(nrThreads);
    for (int i = 0; i < nrThreads; i++) {
      Thread t = new Thread(new Runnable() {
        @Override
        @SuppressFBWarnings("MDM_THREAD_YIELD")
        public void run() {
          try {
            double d = 0;
            while (!stopped) {
              d += doStuff();
              Thread.sleep(1);
            }
            LOG.debug("nr = {}", d);
          } catch (InterruptedException e) {
            // do nothing
          }

        }

        private double doStuff() {
          return getStuff(10) * getStuff(10) * getStuff(10) * getStuff(10);
        }

        private double getStuff(final double nr) {
          return Math.exp(nr);
        }
      }, "Thread" + i);
      t.start();
      threads.add(t);
    }
    return threads;

  }
}
