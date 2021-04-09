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
package org.spf4j.avro.official;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.spf4j.avro.AvroCompatUtils;
import org.spf4j.io.AppendableWriter;

/**
 * Adapter for the official library.
 * @author Zoltan Farkas
 */
public final class OfficialAvroAdapter implements AvroCompatUtils.UtilInterface {

  private final EncoderFactory encFactory = EncoderFactory.get();

  private final DecoderFactory decFactory = DecoderFactory.get();

  @Override
  public Encoder getJsonEncoder(final Schema writerSchema, final OutputStream os) throws IOException {
    return encFactory.jsonEncoder(writerSchema, os);
  }

  @Override
  public Encoder getJsonEncoder(final Schema writerSchema, final Appendable os) throws IOException {
    return encFactory.jsonEncoder(writerSchema, new AppendableWriter(os));
  }


  @Override
  public Schema.Field createField(final String name, final Schema schema, final String doc,
          final Object defaultVal,
          final boolean validateDefault, final boolean validateName, final Schema.Field.Order order) {
    return new Schema.Field(name, schema, doc, defaultVal);
  }

  @Override
  public Schema createRecordSchema(final String name, final String doc, final String namespace,
          final boolean isError, final List<Schema.Field> fields, final boolean validateName) {
    return Schema.createRecord(name, doc, namespace, isError, fields);
  }

  @Override
  public Schema createRecordSchema(final String name,
          final String doc, final String namespace, final boolean isError, final boolean validateName) {
    return Schema.createRecord(name, doc, namespace, isError);
  }

  @Override
  public Decoder getJsonDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    return decFactory.jsonDecoder(writerSchema, is);
  }

  @Override
  public String toString() {
    return "OfficialAvroAdapter{" + "encFactory=" + encFactory + ", decFactory=" + decFactory + '}';
  }

}
