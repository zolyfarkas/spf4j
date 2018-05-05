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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.ssdump2.Converter;

public final class SsdumpTest {

  static {
    System.setProperty("spf4j.execContentFactoryWrapperClass",
            "org.spf4j.stackmonitor.ProfiledExecutionContextFactory");
  }

  private static final Logger LOG = LoggerFactory.getLogger(SsdumpTest.class);


  @Test
  public void testDumpExecContexts() throws InterruptedException, IOException {
    ProfiledExecutionContextFactory contextFactory =
            (ProfiledExecutionContextFactory) ExecutionContexts.getContextFactory();

    Sampler sampler = new Sampler(1, (t) -> new ThreadStackSampler(contextFactory::getCurrentThreads));
    Map<String, SampleNode> collected = sampleTest(sampler, "ecStackSample");
    Assert.assertEquals("Actual: " + collected,  1, collected.size());
  }

  @Test
  public void testTracingDumpExecContexts() throws InterruptedException, IOException {
    ProfiledExecutionContextFactory contextFactory =
            (ProfiledExecutionContextFactory) ExecutionContexts.getContextFactory();

    Sampler sampler = new Sampler(1,
            (t) -> new TracingExecutionContextStackCollector(contextFactory::getCurrentThreadContexts));
    Map<String, SampleNode> collected = sampleTest(sampler, "ecTracingStackSample");
    Assert.assertThat(collected.keySet(), Matchers.hasItems(
            Matchers.equalTo("testThread"), Matchers.containsString("org.spf4j.stackmonitor.SsdumpTest")));

  }



  @Test
  public void testDumpDefault() throws InterruptedException, IOException {
    Sampler sampler = new Sampler(1);
    sampleTest(sampler, "stackSample");
  }

  public Map<String, SampleNode> sampleTest(final Sampler sampler, final String filename)
          throws InterruptedException, IOException {
    sampler.registerJmx();
    sampler.start();
    MonitorTest.main(new String[]{});
    final File serializedFile = File.createTempFile(filename, ".ssdump3");
    Map<String, SampleNode> collected = sampler.getStackCollectionsAndReset();
    Converter.saveLabeledDumps(serializedFile, collected);
    LOG.debug("Dumped to file {}", serializedFile);
    sampler.stop();
    Map<String, SampleNode> loadedDumps = Converter.loadLabeledDumps(serializedFile);
    for (Map.Entry<String, SampleNode> entry : loadedDumps.entrySet()) {
      LOG.debug("Loaded {}", entry.getKey());
    }
    return collected;
  }
}
