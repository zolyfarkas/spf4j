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
package org.spf4j.avro.zfork;

import org.spf4j.avro.Yaml;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.ExtendedJsonDecoder;
import org.apache.avro.io.ExtendedJsonEncoder;
import org.spf4j.avro.Adapter;
import org.spf4j.avro.SchemaResolver;

/**
 * Adapter for the zolyfarkas/avro fork.
 * @author Zoltan Farkas
 */
public final class ZForkAvroAdapter implements Adapter {

  @Override
  public Encoder getJsonEncoder(final Schema writerSchema, final OutputStream os) throws IOException {
    return new ExtendedJsonEncoder(writerSchema, os);
  }

  @Override
  public Encoder getJsonEncoder(final Schema writerSchema, final Appendable os) throws IOException {
    return new ExtendedJsonEncoder(writerSchema,
            Schema.FACTORY.createGenerator(CharStreams.asWriter(os)));
  }

  @Override
  public Schema.Field createField(final String name, final Schema schema, final String doc,
          final Object defaultVal,
          final boolean validateDefault, final boolean validateName, final Schema.Field.Order order) {
    return new Schema.Field(name, schema, doc, defaultVal, validateDefault, validateName, order);
  }

  @Override
  public Schema createRecordSchema(final String name, final String doc,
          final String namespace, final boolean isError, final List<Schema.Field> fields, final boolean validateName) {
    return Schema.createRecord(name, doc, namespace, isError, fields, validateName);
  }

  @Override
  public Schema createRecordSchema(final String name, final String doc,
          final String namespace, final boolean isError, final boolean validateName) {
    return Schema.createRecord(name, doc, namespace, isError, validateName);
  }

  @Override
  public Decoder getJsonDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    return new ExtendedJsonDecoder(writerSchema, is, true);
  }

  @Override
  public Schema parseSchema(final Reader reader,
          final boolean allowUndefinedLogicalTypes, final  SchemaResolver resolver)
          throws IOException {
    return new Schema.Parser(new AvroNamesRefResolver(new SchemaResolverAdapter(resolver)))
            .setValidate(true).parse(Schema.FACTORY.createParser(reader), allowUndefinedLogicalTypes);
  }

  @Override
  public Schema parseSchema(final Reader reader) throws IOException {
     return new Schema.Parser().parse(Schema.FACTORY.createParser(reader), true);
  }

  @Override
  public Decoder getJsonDecoder(final Schema writerSchema, final Reader reader) throws IOException {
    return new ExtendedJsonDecoder(writerSchema,
                Schema.FACTORY.createParser(reader), true);
  }

  @Override
  public Decoder getYamlDecoder(final Schema writerSchema, final Reader reader) throws IOException {
     return new ExtendedJsonDecoder(writerSchema,
                Yaml.newParser(reader), true);
  }

  @Override
  public Decoder getJsonDecoder(final Schema writerSchema, final JsonParser parser) throws IOException {
         return new ExtendedJsonDecoder(writerSchema, parser, true);
  }

  @Override
  public boolean isCompatible() {
    try {
      Schema.Field.class.getConstructor(String.class, Schema.class,
              String.class,  Object.class,
              boolean.class, boolean.class, Schema.Field.Order.class);
      return true;
    } catch (NoSuchMethodException | SecurityException ex) {
      return false;
    }
  }

  private static class SchemaResolverAdapter implements org.apache.avro.SchemaResolver {

    private final SchemaResolver resolver;

    SchemaResolverAdapter(final SchemaResolver resolver) {
      this.resolver = resolver;
    }

    @Override
    public Schema resolveSchema(final String id) {
      return resolver.resolveSchema(id);
    }

    @Override
    public String getId(final Schema schema) {
      return resolver.getId(schema);
    }
  }

}
