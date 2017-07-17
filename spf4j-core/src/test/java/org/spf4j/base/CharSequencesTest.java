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
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("LSC_LITERAL_STRING_COMPARISON")
public class CharSequencesTest {


  @Test
  public void testLineNumbering() {
    CharSequence lineNumbered = CharSequences.toLineNumbered(0, "a\nbla\nc");
    Assert.assertEquals("/* 0 */ a\n/* 1 */ bla\n/* 2 */ c", lineNumbered.toString());
  }


  @Test
  public void testID() {
    Assert.assertFalse(JavaUtils.isJavaIdentifier(""));
    Assert.assertFalse(JavaUtils.isJavaIdentifier(null));
    Assert.assertFalse(JavaUtils.isJavaIdentifier("12A"));
    Assert.assertTrue(JavaUtils.isJavaIdentifier("_a"));
    Assert.assertTrue(JavaUtils.isJavaIdentifier("a"));
    Assert.assertTrue(JavaUtils.isJavaIdentifier("a123FGH"));
  }


  @Test
  public void testCompare() {
    Assert.assertEquals(0, CharSequences.compare("blabla", 6, "blabla/cucu", 6));
  }

  @Test
  public void testCompare2() {
    Assert.assertEquals(0, CharSequences.compare("cacablabla", 4, 6, "ablabla/cucu", 1, 6));
  }

  @Test
  public void testCompare3() {
    Assert.assertEquals("blabla123".compareTo("blabla"),
            CharSequences.compare("cacablabla123", 4, 9, "ablabla/cucu", 1, 6));
  }

  @Test
  public void testCompare4() {
    Assert.assertEquals("bla".compareTo("lab"),
            CharSequences.compare("cacablabla123", 4, 3, "ablabla/cucu", 2, 3));
  }

  @Test
  public void testCompare5() {
    Assert.assertEquals("bla".compareTo("labl"),
            CharSequences.compare("cacablabla123", 4, 3, "ablabla/cucu", 2, 4));
  }

}
