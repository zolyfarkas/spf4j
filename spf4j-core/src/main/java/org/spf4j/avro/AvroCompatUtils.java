/*
 * Copyright 2019 SPF4J.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.spf4j.avro.official.OfficialAvroAdapter;
import org.spf4j.avro.zfork.ZForkAvroAdapter;
import org.spf4j.base.Reflections;
/**
 * A set of Utils to abstract away differences between zolyfarkas/avro forrk and apache/avro
 * @author Zoltan Farkas
 */
public final class AvroCompatUtils {

  private static final Adapter ADAPTER;

  static {
    Constructor<?> c = Reflections.getConstructor(Schema.Field.class, String.class, Schema.class,
            String.class,  Object.class,
            boolean.class, boolean.class, Schema.Field.Order.class);
    ADAPTER = c == null ? new OfficialAvroAdapter() : new ZForkAvroAdapter();
  }

  private AvroCompatUtils() {
  }

  public interface Adapter {

    Schema parseSchema(Reader reader)
            throws IOException;

    Schema parseSchema(Reader reader, boolean allowUndefinedLogicalTypes, SchemaResolver resolver)
            throws IOException;

    Schema.Field createField(String name, Schema schema, String doc,
            Object defaultVal,
            boolean validateDefault, boolean validateName, Schema.Field.Order order);

    Schema createRecordSchema(String name, String doc, String namespace,
                                    boolean isError, List<Schema.Field> fields, boolean validateName);

    Schema createRecordSchema(String name, String doc, String namespace,
                                    boolean isError,  boolean validateName);

    Encoder getJsonEncoder(Schema writerSchema, OutputStream os) throws IOException;

    Encoder getJsonEncoder(Schema writerSchema, Appendable os) throws IOException;

    Decoder getJsonDecoder(Schema writerSchema, InputStream is) throws IOException;

    Decoder getJsonDecoder(Schema writerSchema, Reader reader) throws IOException;

    Decoder getYamlDecoder(Schema writerSchema, Reader reader) throws IOException;


  }

  public static Schema.Field createField(final String name, final Schema schema, final String doc,
          final Object defaultVal,
          final boolean validateDefault, final boolean validateName, final Schema.Field.Order order) {
    return ADAPTER.createField(name, schema, doc, defaultVal, validateDefault, validateName, order);
  }

  public static Schema createRecordSchema(final String name, final String doc, final String namespace,
                                    final boolean isError, final List<Schema.Field> fields,
                                    final boolean validateName) {
    return ADAPTER.createRecordSchema(name, doc, namespace, isError, fields, validateName);
  }


  public static Schema createRecordSchema(final String name, final String doc, final String namespace,
                                    final boolean isError, final boolean validateName) {
    return ADAPTER.createRecordSchema(name, doc, namespace, isError, validateName);
  }

  public static Encoder getJsonEncoder(final Schema writerSchema, final OutputStream os) throws IOException {
    return ADAPTER.getJsonEncoder(writerSchema, os);
  }

  public static Encoder getJsonEncoder(final Schema writerSchema, final Appendable os) throws IOException {
    return ADAPTER.getJsonEncoder(writerSchema, os);
  }

  public static Decoder getJsonDecoder(final Schema writerSchema, final InputStream is) throws IOException {
     return ADAPTER.getJsonDecoder(writerSchema, is);
  }

  public static Adapter getAdapter() {
    return ADAPTER;
  }

}
