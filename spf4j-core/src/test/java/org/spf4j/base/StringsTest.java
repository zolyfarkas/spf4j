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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.log.LogRecord;
import org.spf4j.test.log.TestLoggers;

/**
 *
 * @author zoly
 */
public final class StringsTest {

  @Test
  public void testDistance() {
    Assert.assertEquals(3, Strings.distance("abc", "abcdef"));
    Assert.assertEquals(3, Strings.distance("def", "abcdef"));
    Assert.assertEquals(1, Strings.distance("abc", "bc"));
    Assert.assertEquals(3, Strings.distance("abc", "def"));
    Assert.assertEquals(1, Strings.distance("zoltran", "zoltan"));
  }

  @Test
  public void testEscaping() {
    String res = Strings.unescape("a\\n");
    Assert.assertEquals("a\n", res);
  }

  @Test
  public void testUnsafeOps() {
    LogAssert dontExpect = TestLoggers.sys()
            .dontExpect(Strings.class.getName(), Level.INFO,  Matchers.any(LogRecord.class));
    String testString = "dfjgdjshfgsjdhgfskhdfkdshf\ndfs@#$%^&\u63A5\u53D7*($%^&*()(*&^%$#@!>::><>?{PLKJHGFDEWSDFG";
    char[] chars = Strings.steal(testString);
    String otherString = Strings.wrap(chars);
    Assert.assertEquals(testString, otherString);

    byte[] bytes = Strings.toUtf8(testString);
    otherString = Strings.fromUtf8(bytes);
    Assert.assertEquals(testString, otherString);
    dontExpect.assertObservation();
  }

  @Test
  public void testSubSequence() {
    String str = "dsfhjgsdjfgwuergfedhgfjhwheriufwiueruhfguyerugfweuyrygfwueyrghfuwoeruhgfdgwsjhfg";
    Assert.assertEquals(0, CharSequences.compare(str, 3, 12, str, 3, 12));
    Assert.assertTrue(Strings.equals(str.subSequence(15, 15), Strings.subSequence(str, 15, 15)));
    Assert.assertTrue(Strings.equals(str.subSequence(0, str.length()), Strings.subSequence(str, 0, str.length())));
    Assert.assertEquals(str.subSequence(3, 15).toString(), Strings.subSequence(str, 3, 15).toString());
  }

  @Test
  public void testEndsWith() {
    Assert.assertTrue(Strings.endsWith("dfjkshfks", "hfks"));
    Assert.assertTrue(Strings.endsWith("dfjkshfks", ""));
    Assert.assertFalse(Strings.endsWith("dfjkshfks", "hfk"));
    Assert.assertTrue(Strings.endsWith("dfjkshfks", "dfjkshfks"));
    Assert.assertFalse(Strings.endsWith("dfjkshfks", "dfjkshfksu"));
  }

  @Test
  public void testEncoding() {
    StringBuilder sb = new StringBuilder();
    Strings.appendUnsignedString(sb, 38, 4);
    Assert.assertEquals(Integer.toHexString(38), sb.toString());
  }

  @Test
  public void testEncoding2() {
    StringBuilder sb = new StringBuilder();
    Strings.appendUnsignedStringPadded(sb, 38, 5, 4);
    Assert.assertEquals(4, sb.length());
  }

  @Test
  public void testJsonEncoding() {
    StringBuilder sb = new StringBuilder();
    sb.append('\"');
    Strings.escapeJsonString("\n\u0008\u0000abc\"", sb);
    sb.append('\"');
    Assert.assertEquals("\"\\n\\b\\u0000abc\\\"\"", sb.toString());
  }

}
