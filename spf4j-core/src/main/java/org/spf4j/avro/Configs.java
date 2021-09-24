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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.spf4j.base.CharSequences;
import org.spf4j.base.Json;
import org.spf4j.base.Pair;
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
  public static <T> T read(final Class<T> type, final SchemaResolver schemaResolver, final Reader... reader)
          throws IOException {
    if (reader.length == 0) {
      return null;
    }
    SpecificData sd = SpecificData.get();
    Schema rSchema = sd.getSchema(type);

    int l = reader.length - 1;
    ConfigHeader header = parseHeader(reader[l], rSchema, schemaResolver);
    JsonNode node = readTree(header.getReader(), header.getMediaType());
    Schema wSchema = header.getwSchema();
    for (int i = l - 1; i >= 0; i--) {
      header = parseHeader(reader[i], rSchema, schemaResolver);
      JsonNode newNode = readTree(header.getReader(), header.getMediaType());
      wSchema = header.getwSchema();
      node = override(newNode, node, wSchema);
    }
    DatumReader<T> dr = new SpecificDatumReader<>(wSchema, rSchema);
    Adapter adapter = AvroCompatUtils.getAdapter();
    Decoder decoder = adapter.getJsonDecoder(wSchema, node.traverse());
    return dr.read(null, decoder);

  }

  private static JsonNode readTree(final Reader reader, final MediaType mt) throws IOException {
    if ("application".equals(mt.type()) && "json".equals(mt.subtype())) {
      return Json.MAPPER.readTree(reader);
    } else if ("text".equals(mt.type()) && "yaml".equals(mt.subtype())) {
      return Yaml.MAPPER.readTree(reader);
    } else {
      throw new IllegalArgumentException("Unsupported media type " + mt);
    }
  }

  @Nullable
  public static JsonNode override(@Nullable final JsonNode target,
          @Nullable final JsonNode defaults,
          final Schema targetSchema) {
    if (target == null) {
      return defaults;
    }
    Schema.Type type = targetSchema.getType();
    switch (type) {
      case RECORD:
        ObjectNode newNode = JsonNodeFactory.instance.objectNode();
        if (!(target instanceof ObjectNode)) {
          throw new IllegalArgumentException("Target must be of type Object/record, not: " + target);
        }
        newNode.setAll((ObjectNode) target);
        for (Field field : targetSchema.getFields()) {
           String fieldName  = field.name();
           JsonNode d = defaults.get(fieldName);
           if (d == null) {
             for (String alias : field.aliases()) {
               d = defaults.get(alias);
               if (d != null) {
                 break;
               }
             }
           }
           if (d == null) {
             continue;
           }
           JsonNode targetField = target.get(fieldName);
           if (targetField == null) {
             newNode.set(fieldName, d);
           } else {
             newNode.set(fieldName, override(targetField, d,
                     targetSchema.getField(fieldName).schema()));
           }
        }
        return newNode;
      case MAP:
        newNode = JsonNodeFactory.instance.objectNode();
        newNode.setAll((ObjectNode) target);
        for (Entry<String, JsonNode> field : (Iterable<Entry<String, JsonNode>>) defaults::fields) {
           String fieldName  = field.getKey();
           JsonNode targetField = target.get(fieldName);
           if (targetField == null) {
             newNode.set(fieldName, field.getValue());
           } else {
             newNode.set(fieldName, override(targetField, field.getValue(),
                     targetSchema.getValueType()));
           }
        }
        return newNode;
      case UNION:

      case ARRAY:
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
      case ENUM:
      case FIXED:
      case FLOAT:
      case INT:
      case LONG:
      case NULL:
      case STRING:
        return target;
      default:
        throw new UnsupportedOperationException("Unsupported type: " + type);
    }
  }

  public static Pair<Schema, JsonNode> getMatchingSchema(final JsonNode node, final List<Schema> schemas) {
    if (node.isObject()) {
       Iterator<Entry<String, JsonNode>> it = node.fields();
       if (it.hasNext()) {
         Entry<String, JsonNode> entry = it.next();
         if (it.hasNext()) { // must be a record schema.
           Schema theSchema = getOneSchema(schemas, Schema.Type.RECORD, Schema.Type.MAP);
           return Pair.of(theSchema, node);
         } else {
           Schema s = getOneSchema(schemas, entry.getKey());
           if (s == null) {
              Schema theSchema = getOneSchema(schemas, Schema.Type.RECORD, Schema.Type.MAP);
              return Pair.of(theSchema, node);
           } else {
              return Pair.of(s, entry.getValue());
           }
         }
       } else {
           Schema theSchema = getOneSchema(schemas, Schema.Type.RECORD, Schema.Type.MAP);
           return Pair.of(theSchema, node);
       }
    } else if (node.isNull()) {
      return Pair.of(getOneSchema(schemas, Schema.Type.NULL), node);
    } else if (node.isArray()) {
      return Pair.of(getOneSchema(schemas, Schema.Type.ARRAY), node);
    } else if (node.isTextual()) {
      return Pair.of(getOneSchema(schemas, Schema.Type.STRING,  Schema.Type.BYTES, Schema.Type.FIXED), node);
    } else if (node.isBoolean()) {
      return Pair.of(getOneSchema(schemas, Schema.Type.BOOLEAN), node);
    } else if (node.isNumber()) {
      // direct encoded types not really handled here
      return Pair.of(getOneSchema(schemas,
              Schema.Type.DOUBLE,  Schema.Type.FLOAT, Schema.Type.INT, Schema.Type.LONG), node);
    } else {
      throw new IllegalStateException("IUnsupported node type " + node);
    }
  }

  @Nullable
  private static Schema getOneSchema(final List<Schema> schemas, final String name) {
    for (Schema s : schemas) {
      if (s.getFullName().equals(name)) {
        return s;
      }
    }
   return null;
  }

  private static Schema getOneSchema(final List<Schema> schemas, final Schema.Type type) {
    Schema result = null;
    for (Schema s : schemas) {
      if (s.getType() == type) {
        if (result == null) {
          result = s;
        } else {
          throw new IllegalArgumentException("Must have only one " + type + " schema in " + schemas);
        }
      }
    }
    if (result == null) {
      throw new IllegalArgumentException("No record " + type + "  in " + schemas);
    } else {
      return result;
    }
  }

  private static Schema getOneSchema(final List<Schema> schemas, final Schema.Type... types) {
    Schema result = null;
    for (Schema s : schemas) {
      for (Schema.Type t : types) {
        if (s.getType() == t) {
          if (result == null) {
            result = s;
          } else {
            throw new IllegalArgumentException("Must have only one schema of types: "
                    + Arrays.toString(types) + " in " + schemas);
          }
        }
      }
    }
    if (result == null) {
      throw new IllegalArgumentException("No schema of type: " + Arrays.toString(types) + " in " + schemas);
    } else {
      return result;
    }
  }

  @Nullable
  public static <T> T read(final Class<T> type, final Reader reader)
          throws IOException {
    return read(type, SchemaResolver.NONE, reader);
  }


  public static final class ConfigHeader {

    private final Schema wSchema;

    private final Reader reader;

    private final MediaType mediaType;

    public ConfigHeader(final Schema wSchema, final Reader reader, final MediaType mediaType) {
      this.wSchema = wSchema;
      this.reader = reader;
      this.mediaType = mediaType;
    }

    public Schema getwSchema() {
      return wSchema;
    }

    /**
     * The reader with the content of the config. (header component was consumed)
     * @return
     */
    @CreatesObligation
    public Reader getReader() {
      return reader;
    }

    public MediaType getMediaType() {
      return mediaType;
    }

    @Override
    public String toString() {
      return "ConfigHeader{" + "wSchema=" + wSchema + ", reader=" + reader + ", mediaType=" + mediaType + '}';
    }

  }

  public static ConfigHeader parseHeader(@WillNotClose final Reader reader, final Schema rSchema,
          final SchemaResolver schemaResolver) throws IOException {
    Schema wSchema;
    MediaType mt;
    PushbackReader pbr = new PushbackReader(reader);
    int firstChar = pbr.read();
    if (firstChar < 0) {
      mt = MediaType.JSON_UTF_8;
      Reader r = pbr;
      wSchema = rSchema;
      return new ConfigHeader(wSchema, r, mt);
    }
    BufferedReader content;
    Adapter adapter = AvroCompatUtils.getAdapter();
    if (firstChar == '#') {
      content = new BufferedReader(pbr);
        String header = content.readLine();
        if (header == null) {
            mt = MediaType.JSON_UTF_8;
            pbr.unread(firstChar);
            Reader r = pbr;
            wSchema = rSchema;
            return new ConfigHeader(wSchema, r, mt);
        }
        mt = getConfigMediaType(header);
        if (mt != null) {
          List<String> schemaStrList = mt.parameters().get("avsc");
          if (schemaStrList.isEmpty()) {
            wSchema = rSchema;
          } else {
            wSchema = adapter.parseSchema(new StringReader(schemaStrList.get(0)), true, schemaResolver);
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
    return new ConfigHeader(wSchema, content, mt);
  }


  @Nullable
  public static <T> T read(final Class<T> type, final SchemaResolver schemaResolver, final Reader reader)
          throws IOException {
    SpecificData sd = SpecificData.get();
    Schema rSchema = sd.getSchema(type);
    ConfigHeader header = parseHeader(reader, rSchema, schemaResolver);
    Schema wSchema = header.getwSchema();
    Reader content = header.getReader();
    MediaType mt = header.getMediaType();
    DatumReader<T> dr = new SpecificDatumReader<>(wSchema, rSchema);
    Decoder decoder;
    Adapter adapter = AvroCompatUtils.getAdapter();
    if ("application".equals(mt.type()) && "json".equals(mt.subtype())) {
      decoder = adapter.getJsonDecoder(wSchema, content);
    } else if ("text".equals(mt.type()) && "yaml".equals(mt.subtype())) {
      decoder = adapter.getYamlDecoder(wSchema, content);
    } else {
      throw new IllegalArgumentException("Unsupported media type " + mt);
    }
    return dr.read(null, decoder);
  }

}
