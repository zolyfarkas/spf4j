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
package org.spf4j.perf.tsdb;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.tsdb2.TimeSeries;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({"CLI_CONSTANT_LIST_INDEX", "MDM_THREAD_YIELD", "PATH_TRAVERSAL_IN"})
public final class TimeSeriesDatabaseTest {

  private static final Logger LOG = LoggerFactory.getLogger(TimeSeriesDatabaseTest.class);

  private static final String FILE_NAME = System.getProperty("java.io.tmpdir") + "/testdb.tsdb";

  /**
   * Test of close method, of class TimeSeriesDatabase.
   */
  @Test
  public void testWriteTSDB() throws Exception {
    if (new File(FILE_NAME).delete()) {
      LOG.debug("existing tsdb {} file deleted", FILE_NAME);
    }
    try (TimeSeriesDatabase instance = new TimeSeriesDatabase(new File(FILE_NAME), new byte[]{})) {
      instance.addTSTable("gr1", new byte[]{}, 5, new String[]{"a", "b"}, new byte[][]{});
      instance.write(System.currentTimeMillis(), "gr1", new long[]{0, 1});
      Thread.sleep(5);
      instance.write(System.currentTimeMillis(), "gr1", new long[]{1, 2});
      Thread.sleep(5);
      instance.write(System.currentTimeMillis(), "gr1", new long[]{3, 4});
      Thread.sleep(5);
      instance.addTSTable("gr2", new byte[]{}, 5, new String[]{"a", "b"}, new byte[][]{});
      instance.write(System.currentTimeMillis(), "gr2", new long[]{7, 8});
      instance.flush();

      instance.addTSTable("gr3", new byte[]{}, 5, new String[]{"a", "b"}, new byte[][]{});
      instance.write(System.currentTimeMillis(), "gr3", new long[]{7, 8});
      Thread.sleep(5);
      instance.write(System.currentTimeMillis(), "gr3", new long[]{9, 10});
      instance.flush();
      LOG.debug("TSTables = {}", instance.getTSTables());
      TimeSeries readAll = instance.readAll("gr1");
      Assert.assertEquals(2, readAll.getValues()[1][1]);
      LOG.debug("TimeSeries = {}", readAll);
      readAll = instance.readAll("gr2");
      Assert.assertEquals(7, readAll.getValues()[0][0]);
      LOG.debug("TimeSeries = {}", readAll);
      readAll = instance.readAll("gr3");
      Assert.assertEquals(10, readAll.getValues()[1][1]);
      LOG.debug("TimeSeries = {}", readAll);
    }

    TimeSeriesDatabase instanceRead = new TimeSeriesDatabase(new File(FILE_NAME), null);
    Collection<TSTable> tsTables = instanceRead.getTSTables();

    LOG.debug("Tables = {}", tsTables);
    Assert.assertEquals(3, tsTables.size());
    instanceRead.writeCsvTable("gr1", File.createTempFile("test", ".csv"));
    instanceRead.writeCsvTables(Arrays.asList("gr1", "gr2", "gr3"), File.createTempFile("testAll", ".csv"));

  }

  @Test(expected = IllegalArgumentException.class)
  public void testWriteBadTSDB() throws Exception {
    try (TimeSeriesDatabase instance = new TimeSeriesDatabase(new File(FILE_NAME), new byte[]{})) {
      instance.addTSTable("gr1", new byte[]{}, 5, new String[]{"a", "b"}, new byte[][]{});
      instance.addTSTable("gr1", new byte[]{}, 5, new String[]{"a", "b"}, new byte[][]{});
    }
  }

}
