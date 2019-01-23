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
package org.spf4j.io.csv;

import java.io.IOException;
import java.io.StringReader;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class CharSeparatedValuesTest {


  @Test
  public void testCsvReader() throws IOException, CsvParseException {
    CharSeparatedValues csv = new CharSeparatedValues(' ');
    CsvReader reader = csv.reader(new StringReader("a b c\nd e"));
    Assert.assertEquals(CsvReader.TokenType.ELEMENT, reader.next());
    Assert.assertEquals("a", reader.getElement().toString());
    Assert.assertEquals(CsvReader.TokenType.ELEMENT, reader.next());
    Assert.assertEquals("b", reader.getElement().toString());
    Assert.assertEquals(CsvReader.TokenType.ELEMENT, reader.next());
    Assert.assertEquals("c", reader.getElement().toString());
    Assert.assertEquals(CsvReader.TokenType.END_ROW, reader.next());
    Assert.assertEquals(CsvReader.TokenType.END_ROW, reader.current());
    Assert.assertEquals(CsvReader.TokenType.ELEMENT, reader.next());
    Assert.assertEquals(CsvReader.TokenType.ELEMENT, reader.current());
    Assert.assertEquals("d", reader.getElement().toString());
    Assert.assertEquals(CsvReader.TokenType.ELEMENT, reader.next());
    Assert.assertEquals("e", reader.getElement().toString());
    Assert.assertEquals(CsvReader.TokenType.END_DOCUMENT, reader.next());
  }

  @Test
  public void testCsvReader2() throws IOException, CsvParseException {
    CharSeparatedValues csv = new CharSeparatedValues(' ');
    CsvReader reader = csv.reader(new StringReader(""));
    Assert.assertNull(reader.current());
    Assert.assertEquals(CsvReader.TokenType.ELEMENT, reader.next());
    Assert.assertEquals("", reader.getElement().toString());
    Assert.assertEquals(CsvReader.TokenType.END_DOCUMENT, reader.next());
    Assert.assertEquals(CsvReader.TokenType.END_DOCUMENT, reader.current());
  }

}
