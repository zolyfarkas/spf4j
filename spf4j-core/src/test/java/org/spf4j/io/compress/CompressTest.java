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
package org.spf4j.io.compress;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class CompressTest {

  @Test
  public void testZip() throws IOException {
    File tmpFolder = Files.createTempDir();
    File file = File.createTempFile(".bla", ".tmp", tmpFolder);
    String testStr = "skjfghskjdhgfjgishfgksjhgjkhdskghsfdkjhg";
    File subFolder = new File(file.getParent(), "subFolder");
    if (!subFolder.mkdir()) {
      throw new IOException("Cannot create folder " + subFolder);
    }
    File subTestFile = new File(subFolder, "subTestFile.txt");
    Files.asCharSink(file, StandardCharsets.UTF_8).write(testStr);
    Files.asCharSink(subTestFile, StandardCharsets.UTF_8).write(testStr);
    Path zip = Compress.zip(tmpFolder.toPath());
    Assert.assertThat(zip.getFileName().toString(), Matchers.endsWith(".zip"));
    Assert.assertTrue(java.nio.file.Files.exists(zip));
    File tmpFir = Files.createTempDir();
    List<Path> unzip = Compress.unzip(zip, tmpFir.toPath());
    Assert.assertEquals(2, unzip.size());
    Assert.assertEquals(testStr, Files.asCharSource(unzip.get(0).toFile(), StandardCharsets.UTF_8).read());
    Assert.assertEquals(testStr, Files.asCharSource(unzip.get(1).toFile(), StandardCharsets.UTF_8).read());
  }

  @Test
  public void testZip2() throws IOException {
    File file = File.createTempFile(".bla", ".tmp");
    String testStr = "skjfghskjdhgfjgishfgksjhgjkhdskghsfdkjhg";
    Files.write(testStr, file, StandardCharsets.UTF_8);
    Path zip = Compress.zip(file.toPath());
    Assert.assertThat(zip.getFileName().toString(), Matchers.endsWith(".zip"));
    Assert.assertTrue(java.nio.file.Files.exists(zip));
    File tmpFir = Files.createTempDir();
    List<Path> unzip = Compress.unzip(zip, tmpFir.toPath());
    Assert.assertEquals(1, unzip.size());
    Assert.assertEquals(testStr, Files.toString(unzip.get(0).toFile(), StandardCharsets.UTF_8));
  }



}
