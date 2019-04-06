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
package org.spf4j.avro.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.io.csv.CsvReader;
import org.spf4j.avro.schema.Schemas;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class DecodedSchema {

  private final Schema schema;

  private final Decoder decoder;

  public DecodedSchema(final Schema schema, final Decoder decoder) {
    this.schema = schema;
    this.decoder = decoder;
  }

  public Schema getSchema() {
    return schema;
  }

  public Decoder getDecoder() {
    return decoder;
  }

  @Override
  public String toString() {
    return "DecodedSchema{" + "schema=" + schema + ", decoder=" + decoder + '}';
  }

  private static String validateName(final CharSequence name) {
    int length = name.length();
    if (length == 0) {
      return "_";
    }
    StringBuilder result = null;
    char first = name.charAt(0);
    if (first != '_' && !Character.isLetter(first)) {
      result = new StringBuilder(length);
      result.append('_');
    }
    for (int i = 1; i < length; i++) {
      char c = name.charAt(i);
      if (c != '_' && !Character.isLetterOrDigit(c)) {
        if (result == null) {
          result = new StringBuilder(length);
          result.append(name, 0, i);
        }
        result.append('_');
      } else if (result != null) {
        result.append(c);
      }
    }
    return result == null ? name.toString() : result.toString();
  }

  public static DecodedSchema tryDecodeSchema(final InputStream is, @Nullable final Schema readerSchema)
          throws IOException {
    try {
      CsvReader reader = Csv.CSV.readerILEL(new InputStreamReader(is, StandardCharsets.UTF_8));
      Schema record;
      if (readerSchema == null) {
        record = Schema.createRecord("DynCsv", "Infered schema", "org.spf4j.avro", false);
        List<Schema.Field> bfields = new ArrayList<>();
        reader.readRow((cs) -> {
          bfields.add(new Schema.Field(validateName(cs), Schema.create(Schema.Type.STRING),
                  cs.toString(), (Object) null));
        });
        record.setFields(bfields);
      } else {
        List<CharSequence> list = new ArrayList<>();
        reader.readRow((cs) -> list.add(cs.toString()));
        record = Schemas.project(readerSchema.getElementType(), list.toArray(new CharSequence[list.size()]));
      }
      Schema arraySchema = Schema.createArray(record);
      return new DecodedSchema(arraySchema, new CsvDecoder(reader, arraySchema));
    } catch (CsvParseException ex) {
      throw new RuntimeException(ex);
    }
  }

}
