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
package org.spf4j.avro;

import com.google.common.net.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.spf4j.base.CharSequences;
import org.spf4j.io.PushbackReader;

/**
 * Utilities to read Objects from configuration files enforced via json/yaml files.
 *
 * @author Zoltan Farkas
 */
public final class Configs {

  private Configs() { }

  @Nullable
  private static MediaType getConfigMediaType(final CharSequence seq) {
    if (!CharSequences.startsWith(seq, "Content-Type:", 0)) {
      return null;
    }
    int length = seq.length();
    int indexOf = CharSequences.indexOf(seq, 0, length, ':');
    int next = indexOf + 1;
    int endMt = CharSequences.indexOf(seq, next, length, '\n');
    if (endMt < 0) {
      endMt = length;
    }
    return MediaType.parse(seq.subSequence(next, endMt).toString());
  }

  @Nullable
  public static <T> T read(final Reader reader, final Class<T> type)
          throws IOException {
    SpecificData sd = SpecificData.get();
    Schema rSchema = sd.getSchema(type);
    Schema wSchema;
    PushbackReader pbr = new PushbackReader(reader);
    int firstChar = pbr.read();
    if (firstChar < 0) {
      return null;
    }
    BufferedReader content;
    AvroCompatUtils.Adapter adapter = AvroCompatUtils.getAdapter();
    MediaType mt;
    if (firstChar == '#') {
      content = new BufferedReader(pbr);
      String header = content.readLine();
      if (header == null) {
        return null;
      }
      mt = getConfigMediaType(header);
      if (mt != null) {
        List<String> schemaStrList = mt.parameters().get("avsc");
        if (schemaStrList.isEmpty()) {
          wSchema = rSchema;
        } else {
          wSchema = adapter.parseSchema(new StringReader(schemaStrList.get(0)));
        }
      } else {
         mt = MediaType.create(header, header);
         wSchema = rSchema;
      }
    } else {
      mt = MediaType.JSON_UTF_8;
      pbr.unread(firstChar);
      content = new BufferedReader(pbr);
      wSchema = rSchema;
    }
    DatumReader<T> dr = new SpecificDatumReader<>(wSchema, rSchema);
    try {
      Decoder decoder;
      if ("application".equals(mt.type()) && "json".equals(mt.subtype())) {
        decoder  = adapter.getJsonDecoder(wSchema, content);
      } else if ("text".equals(mt.type()) && "yaml".equals(mt.subtype())) {
        decoder = adapter.getYamlDecoder(wSchema, content);
      } else {
        throw new IllegalArgumentException("Unsupported media type " + mt);
      }
      return dr.read(null, decoder);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }

  }

}
