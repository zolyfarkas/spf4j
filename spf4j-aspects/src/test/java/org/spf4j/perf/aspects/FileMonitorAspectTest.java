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
package org.spf4j.perf.aspects;

import com.google.common.collect.ListMultimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.os.OperatingSystem;
import org.spf4j.perf.impl.RecorderFactory;
import org.spf4j.perf.impl.ms.tsdb.TSDBMeasurementStore;
import org.spf4j.tsdb2.TSDBQuery;
import org.spf4j.tsdb2.TSDBQuery.TableDefEx;
import org.spf4j.tsdb2.TSDBWriter;

/**
 * @author zoly
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public final class FileMonitorAspectTest {

  private static final Logger LOG = LoggerFactory.getLogger(FileMonitorAspectTest.class);

  /**
   * Test of nioReadLong method, of class FileMonitorAspect.
   */
  @Test
  public void testFileIOMonitoring() throws Exception {
    System.setProperty("spf4j.perf.file.sampleTimeMillis", "1000");
    File tempFile = File.createTempFile("test", ".tmp");
    tempFile.deleteOnExit();
    try (Writer fw = new OutputStreamWriter(Files.newOutputStream(tempFile.toPath()), StandardCharsets.UTF_8)) {
      for (int i = 0; i < 10; i++) {
        fw.write("bla bla test\n");
        Thread.sleep(500);
      }
    }
    Thread.sleep(1000);
    try (BufferedReader fr = new BufferedReader(new InputStreamReader(
            Files.newInputStream(tempFile.toPath()), StandardCharsets.UTF_8))) {
      String line = fr.readLine();
      while (line != null) {
        LOG.debug("Read: {}", line);
        line = fr.readLine();
        Thread.sleep(500);
      }
    }
    TSDBWriter dbWriter = ((TSDBMeasurementStore) RecorderFactory.MEASUREMENT_STORE).getDBWriter();
    dbWriter.flush();
    final File file = dbWriter.getFile();
    ListMultimap<String, TableDefEx> allTables = TSDBQuery.getAllTablesWithDataRanges(file);
    LOG.debug("Tables {}", allTables);
    Map<String, Collection<TableDefEx>> asMap = allTables.asMap();
    Assert.assertThat(asMap, (Matcher)
            Matchers.hasKey("file-write,org.spf4j.perf.aspects.FileMonitorAspectTest"));
    Assert.assertThat(asMap, (Matcher)
            Matchers.hasKey("file-read,org.spf4j.perf.aspects.FileMonitorAspectTest"));
    List<TableDefEx> get = allTables.get("file-write,org.spf4j.perf.aspects.FileMonitorAspectTest");
    Assert.assertTrue(get.get(0).getStartTime() != 0);
    Assert.assertTrue(OperatingSystem.getOpenFileDescriptorCount() > 0);
  }

}
