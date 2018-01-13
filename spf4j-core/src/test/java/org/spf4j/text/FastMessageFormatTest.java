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
package org.spf4j.text;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class FastMessageFormatTest {

  @Test
  public void testFormatter() throws IOException {
    StringBuilder sb = new StringBuilder();
    MessageFormat format = new MessageFormat("{0}, {1}");
    format.format(new Object[]{"a", "b"}, sb, null);
    Assert.assertEquals("a, b", sb.toString());
  }

  @Test
  public void testFormatter5() throws IOException {
    StringBuilder sb = new StringBuilder();
    MessageFormat format = new MessageFormat("{0}, {1}, {2}, {3}, {4}");
    format.format(new Object[]{"a", "b", "c", "d", "e"}, sb, null);
    Assert.assertEquals("a, b, c, d, e", sb.toString());
  }

  @Test
  public void testFormatter2() throws IOException {
    StringBuilder sb = new StringBuilder();
    MessageFormat format = new MessageFormat("bla bla bla");
    format.format(null, sb, null);
    Assert.assertEquals("bla bla bla", sb.toString());
  }

  @Test
  public void testFormatter3() throws IOException {
    StringBuilder sb = new StringBuilder();
    MessageFormat format = new MessageFormat("pre {1}, {0} suf");
    format.format(new Object[]{"a", "b"}, sb, null);
    Assert.assertEquals("pre b, a suf", sb.toString());
  }

  @Test
  public void testFormatter4() throws IOException {
    StringBuilder sb = new StringBuilder();
    MessageFormat format = new MessageFormat("pre {1}, {0}, {2,number,$'#',##} suf");
    format.format(new Object[]{"a", "b", 100}, sb, null);
    Assert.assertEquals("pre b, a, $#1,00 suf", sb.toString());
  }

}
