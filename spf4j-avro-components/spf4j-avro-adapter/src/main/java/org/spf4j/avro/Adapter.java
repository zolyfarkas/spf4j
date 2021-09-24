/*
 * Copyright 2021 SPF4J.
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

import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;

/**
 *
 * @author Zoltan Farkas
 */
public interface Adapter {

  boolean isCompatible();

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
          boolean isError, boolean validateName);

  Encoder getJsonEncoder(Schema writerSchema, OutputStream os) throws IOException;

  Encoder getJsonEncoder(Schema writerSchema, Appendable os) throws IOException;

  Decoder getJsonDecoder(Schema writerSchema, InputStream is) throws IOException;

  Decoder getJsonDecoder(Schema writerSchema, Reader reader) throws IOException;

  Decoder getJsonDecoder(Schema writerSchema, JsonParser parser) throws IOException;

  Decoder getYamlDecoder(Schema writerSchema, Reader reader) throws IOException;

}
