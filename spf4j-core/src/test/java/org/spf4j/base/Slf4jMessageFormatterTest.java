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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.io.AppendableLimiterWithOverflow;
import org.spf4j.io.ConfigurableAppenderSupplier;
import org.spf4j.io.ObjectAppender;
import org.spf4j.ssdump2.avro.AMethod;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class Slf4jMessageFormatterTest {

  private static final Logger LOG = LoggerFactory.getLogger(Slf4jMessageFormatterTest.class);

  @Test
  public void testFormatter() throws IOException {
    StringBuilder sb = new StringBuilder();
    Slf4jMessageFormatter.format(sb, "bla bla");
    Assert.assertEquals("bla bla", sb.toString());
    sb.setLength(0);
    Slf4jMessageFormatter.format(sb, "bla bla {}", "coco");
    Assert.assertEquals("bla bla coco", sb.toString());
    sb.setLength(0);
    Slf4jMessageFormatter.format(sb, "\\{}bla bla {}", "coco");
    Assert.assertEquals("{}bla bla coco", sb.toString());
    sb.setLength(0);
    int processed = Slf4jMessageFormatter.format(sb, "Some Message", "coco");
    Assert.assertEquals("Some Message", sb.toString());
    Assert.assertEquals(0, processed);
    sb.setLength(0);
    processed = Slf4jMessageFormatter.format(sb, "Some Message {}", new ConfigurableAppenderSupplier(),
            Pair.of("a", "b"));
    Assert.assertEquals("Some Message a,b", sb.toString());
    Assert.assertEquals(1, processed);
  }

  @Test
  public void testFormatter2() throws IOException {
    ConfigurableAppenderSupplier appSupp = new ConfigurableAppenderSupplier();
    LOG.debug("ConfAppenderSupp = {}", appSupp);
    StringBuilder sb = new StringBuilder();
    final long currentTimeMillis = System.currentTimeMillis();
    Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp, new java.sql.Date(currentTimeMillis));
    Assert.assertEquals("bla bla " + DateTimeFormats.DT_FORMAT.format(Instant.now()), sb.toString());
    sb.setLength(0);
    AMethod method = AMethod.newBuilder().setName("m1").setDeclaringClass("c1").build();
    int written = Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp, method);
    Assert.assertEquals("bla bla {\"declaringClass\":\"c1\",\"name\":\"m1\"}", sb.toString());
    Assert.assertEquals(1, written);
    sb.setLength(0);
    written = Slf4jMessageFormatter.format(1, sb, "bla bla {}", appSupp, "ifff", method);
    Assert.assertEquals("bla bla {\"declaringClass\":\"c1\",\"name\":\"m1\"}", sb.toString());
    Assert.assertEquals(2, written);
    sb.setLength(0);
    written = Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp, method, "yohooo");
    LOG.debug("formatted message: {}", sb);
    Assert.assertEquals(1, written);
    sb.setLength(0);
    written = Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp);
    LOG.debug("formatted message: {}", sb);
    Assert.assertEquals(0, written);
    sb.setLength(0);
    EscapeJsonStringAppendableWrapper escaper = new EscapeJsonStringAppendableWrapper(sb);
    Slf4jMessageFormatter.format(escaper, "bla bla {} {}", appSupp, "\n\u2013\u0010",
            new int[]{1, 2, 3});
    LOG.debug("formatted message: {}", sb);
    Assert.assertEquals("bla bla \\n–\\u0010 [1, 2, 3]", sb.toString());
    appSupp.replace(String.class, (final ObjectAppender<? super String> input) -> new ObjectAppender<String>() {
      @Override
      public void append(final String object, final Appendable appendTo) throws IOException {
        try (AppendableLimiterWithOverflow limiter
                = new AppendableLimiterWithOverflow(90, File.createTempFile("string", ".overflow"),
                        "...@", StandardCharsets.UTF_8, appendTo)) {
          limiter.append(object);
        }
      }
    });
    sb.setLength(0);
    Slf4jMessageFormatter.format(sb, "bla bla {}", appSupp,
            "012345678901234567890123456789012345678901234567"
                    + "89012345678901234567890123456789012345678901234567890123456789");
    LOG.debug("formatted message: {}", sb);
    Assert.assertThat(sb.toString(), Matchers.containsString("...@"));
  }

  @Test
  @SuppressFBWarnings({ "LO_INCORRECT_NUMBER_OF_ANCHOR_PARAMETERS", "UCC_UNRELATED_COLLECTION_CONTENTS" })
  public void testFormatterRecursion() throws IOException {
    StringBuilder builder = new StringBuilder();
    Object[] arr = new Object[4];
    arr[0] = "a";
    arr[1] = arr;
    arr[2] = "b";
    arr[3] = arr;
    Slf4jMessageFormatter.format(builder, "{} {}", (Object) arr, arr);
    LOG.debug("", builder);
    Assert.assertEquals("[a, [...], b, [...]] [a, [...], b, [...]]", builder.toString());
  }

}
