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
import java.io.StringWriter;
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

  private static  final String TEST_STR
            = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";


  @Test
  public void testOverflowX() throws IOException {
    testOverflowByX(90, TEST_STR, 0, new StringBuilder());
    testOverflowByX(90, TEST_STR, 8, new StringBuilder());
    testOverflowByX(90, TEST_STR, 13, new StringBuilder());
    testOverflowByX(90, TEST_STR, 88, new StringBuilder());
    testOverflowByX(90, TEST_STR, 90, new StringBuilder());
    testOverflowByX(TEST_STR.length(), TEST_STR, 45, new StringBuilder());
    testOverflowByX(90, TEST_STR, 0, new StringWriter());
    testOverflowByX(90, TEST_STR, 8, new StringWriter());
    testOverflowByX(90, TEST_STR, 13, new StringWriter());
    testOverflowByX(90, TEST_STR, 88, new StringWriter());
    testOverflowByX(90, TEST_STR, 90, new StringWriter());
    testOverflowByX(TEST_STR.length(), TEST_STR, 45, new StringWriter());
  }

  private void testOverflowByX(final int limit, final String testStr, final int splitIdx,
          final Appendable destination) throws IOException {
    File ovflow = File.createTempFile("overflow", ".txt");
    LOG.debug("Destination file: {}", ovflow.getPath());
    try (AppendableLimiterWithOverflow limiter
            = new AppendableLimiterWithOverflow(limit, ovflow, "...@", StandardCharsets.UTF_8, destination)) {
      limiter.append(testStr.subSequence(0, splitIdx));
      limiter.append(testStr.charAt(splitIdx));
      limiter.append(testStr.subSequence(splitIdx + 1, testStr.length()));
    }

    LOG.debug("Result: {}", destination);
    String destinationStr = destination.toString();
    Assert.assertEquals(limit, destinationStr.length());
    String suffix = "...@" + ovflow.getPath();
    if (limit < testStr.length()) {
      Assert.assertThat(destinationStr, Matchers.endsWith(suffix));
      int strWritten = 90 - suffix.length();
      Assert.assertEquals(testStr.substring(0, strWritten), destinationStr.substring(0, strWritten));
      String oContent = CharStreams.toString(new InputStreamReader(Files.newInputStream(ovflow.toPath()),
              StandardCharsets.UTF_8));
      Assert.assertEquals(testStr, oContent);
    } else {
      Assert.assertEquals(testStr, destinationStr);
            String oContent = CharStreams.toString(new InputStreamReader(Files.newInputStream(ovflow.toPath()),
              StandardCharsets.UTF_8));
      Assert.assertEquals("", oContent);
    }
  }

}
