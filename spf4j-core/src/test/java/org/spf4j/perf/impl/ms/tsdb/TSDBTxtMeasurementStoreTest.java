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
package org.spf4j.perf.impl.ms.tsdb;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.perf.impl.MeasurementsInfoImpl;

/**
 * @author zoly
 */
public class TSDBTxtMeasurementStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TSDBTxtMeasurementStoreTest.class);

  @Test
  public void testCreateAppend() throws IOException {
    Path tmpFile = Paths.get(org.spf4j.base.Runtime.TMP_FOLDER, "testM.txt");
    File file = tmpFile.toFile();
    TSDBTxtMeasurementStore store = new TSDBTxtMeasurementStore(file);
    long id = store.alocateMeasurements(new MeasurementsInfoImpl("test", "bla",
            new String[]{"a", "b"}, new String[]{"ms", "ms"}), 10000);
    store.saveMeasurements(id, System.currentTimeMillis(), 3L, 4L);
    store.close();
    String content = Files.lines(tmpFile, StandardCharsets.UTF_8).collect(Collectors.joining("\n"));
    LOG.debug("File Content: {}", content);
    Assert.assertThat(content, Matchers.containsString("a,3,b,4"));
    TSDBTxtMeasurementStore store2 = new TSDBTxtMeasurementStore(file);
    store2.saveMeasurements(id, System.currentTimeMillis(), 5L, 6L);
    store2.close();
    content = Files.lines(tmpFile, StandardCharsets.UTF_8).collect(Collectors.joining("\n"));
    Assert.assertThat(content, Matchers.containsString("a,3,b,4"));
    Assert.assertThat(content, Matchers.containsString("a,5,b,6"));
  }

}
