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
package org.spf4j.io;

import com.google.common.io.CharStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public class AppendableLimiterWithFileOverflowTest {

  private static final Logger LOG = LoggerFactory.getLogger(AppendableLimiterWithFileOverflowTest.class);

  @Test
  public void testOverflow() throws IOException {
    File ovflow = File.createTempFile("overflow", ".txt");
    StringBuilder destination = new StringBuilder();
    final String testStr
            = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

    try (AppendableLimiterWithOverflow limiter
            = new AppendableLimiterWithOverflow(90, ovflow, "...@", StandardCharsets.UTF_8, destination)) {
      limiter.append(testStr.subSequence(0, 45));
      limiter.append(testStr.charAt(45));
      limiter.append(testStr.subSequence(46, testStr.length()));
    }
    LOG.debug("Destination str {}", destination);
    Assert.assertEquals(90, destination.length());
    String oContent = CharStreams.toString(new InputStreamReader(Files.newInputStream(ovflow.toPath()),
            StandardCharsets.UTF_8));
    Assert.assertEquals(testStr, oContent);
  }

  @Test
  public void testOverflowX() throws IOException {
    File ovflow = File.createTempFile("overflow", ".txt");
    StringBuilder destination = new StringBuilder();
    final String testStr
            = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    int nr = ovflow.getPath().length() + 4;
    try (AppendableLimiterWithOverflow limiter
            = new AppendableLimiterWithOverflow(90, ovflow, "...@", StandardCharsets.UTF_8, destination)) {
      limiter.append(testStr.subSequence(0, nr));
      limiter.append(testStr.charAt(nr));
      limiter.append(testStr.subSequence(nr + 1, testStr.length()));
    }

    LOG.debug("Destination: {}", destination);
    Assert.assertEquals(90, destination.length());
    String suffix = "...@" + ovflow.getPath();
    String destinationStr = destination.toString();
    Assert.assertThat(destinationStr, Matchers.endsWith(suffix));
    int strWritten = 90 - suffix.length();
    Assert.assertEquals(testStr.substring(0, strWritten), destinationStr.substring(0, strWritten));
    String oContent = CharStreams.toString(new InputStreamReader(Files.newInputStream(ovflow.toPath()),
            StandardCharsets.UTF_8));
    Assert.assertEquals(testStr, oContent);
  }

  @Test
  public void testOverflow2() throws IOException {
    File ovflow = File.createTempFile("overflow", ".txt");
    StringBuilder destination = new StringBuilder();
    final String testStr
            = "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    try (AppendableLimiterWithOverflow limiter
            = new AppendableLimiterWithOverflow(90, ovflow, "...@", StandardCharsets.UTF_8, destination)) {
      limiter.append(testStr.subSequence(0, 45));
      limiter.append(testStr.charAt(45));
      limiter.append(testStr.subSequence(46, testStr.length()));
    }

    LOG.debug("Destination: {}", destination);
    Assert.assertEquals(90, destination.length());
    Assert.assertEquals(testStr, destination.toString());
    String oContent = CharStreams.toString(new InputStreamReader(Files.newInputStream(ovflow.toPath()),
            StandardCharsets.UTF_8));
    Assert.assertEquals("", oContent);
  }

}
