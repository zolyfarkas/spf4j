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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class CsvReader2Iterator implements Iterator<Iterable<String>> {

  private final CsvReader reader;

  private final List<String> row;

  private boolean haveParsedRow;

  public CsvReader2Iterator(final CsvReader preader) {
    this.reader = preader;
    haveParsedRow = false;
    row = new ArrayList<>();
  }

  private CsvReader.TokenType readRow() throws IOException, CsvParseException {
    row.clear();
    CsvReader.TokenType token;
    boolean loop = true;
    do {
      token = reader.next();
      switch (token) {
        case ELEMENT:
          row.add(reader.getElement().toString());
          break;
        case END_DOCUMENT:
        case END_ROW:
          loop = false;
          break;
        default:
          throw new IllegalStateException("Illegal token " + token);
      }
    } while (loop);
    haveParsedRow = !row.isEmpty();
    return token;
  }

  @Override
  public boolean hasNext() {
    if (!haveParsedRow) {
      try {
        CsvReader.TokenType token = readRow();
        if (haveParsedRow) {
          return true;
        } else {
          return token != CsvReader.TokenType.END_DOCUMENT;
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      } catch (CsvParseException ex) {
        throw new UncheckedCsvParseException(ex);
      }
    } else {
      return true;
    }
  }

  @Override
  public Iterable<String> next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    } else {
      haveParsedRow = false;
      return row;
    }
  }

  @Override
  public String toString() {
    return "CsvReader2Iterator{" + "reader=" + reader + ", row=" + row + ", haveParsedRow=" + haveParsedRow + '}';
  }


}
