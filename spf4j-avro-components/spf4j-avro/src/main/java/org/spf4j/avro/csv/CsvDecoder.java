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

import org.spf4j.avro.DecodedSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Schema;
import org.apache.avro.io.ParsingDecoder;
import org.apache.avro.io.parsing.JsonGrammarGenerator;
import org.apache.avro.io.parsing.Symbol;
import org.apache.avro.util.Utf8;
import org.spf4j.avro.AvroCompatUtils;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.Base64;
import org.spf4j.base.CharSequences;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.io.csv.CsvReader;

/**
 * A Csv encoder for avro records. there are restrictions on the what schema's are supported.
 *
 * @author Zoltan Farkas
 */
public final class CsvDecoder extends ParsingDecoder {

  private final CsvReader csvReader;

  public CsvDecoder(final CsvReader csvReader, final Schema readerSchema) throws IOException {
    super(new JsonGrammarGenerator().generate(readerSchema));
    this.csvReader = csvReader;
    try {
      if (csvReader.current() == CsvReader.TokenType.START_DOCUMENT) {
        csvReader.next();
      }
    } catch (CsvParseException ex) {
      throw new AvroRuntimeException(ex);
    }
  }

  public void skipHeader() throws IOException, CsvParseException {
    csvReader.skipRow();
  }


  @Override
  public void readNull() throws IOException {
    parser.advance(Symbol.NULL);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      boolean notNull = !CharSequences.equals("", csvReader.getElement());
      parseNextCsv();
      if (notNull) {
        throw new AvroTypeException("Expected null, not " + csvReader.getElement());
      }
    } else {
      throw new AvroTypeException("Expected boolean, not " + tok);
    }
  }

  @Override
  public boolean readBoolean() throws IOException {
    parser.advance(Symbol.BOOLEAN);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      boolean result = CharSequences.equals("true", csvReader.getElement());
      parseNextCsv();
      return result;
    } else {
      throw new AvroTypeException("Expected boolean, not " + tok);
    }
  }

  private void parseNextCsv() throws IOException {
    try {
      csvReader.next();
    } catch (CsvParseException ex) {
      throw new AvroRuntimeException(ex);
    }
  }

  @Override
  public int readInt() throws IOException {
    parser.advance(Symbol.INT);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      int result = CharSequences.parseInt(csvReader.getElement());
      parseNextCsv();
      return result;
    } else {
      throw new AvroTypeException("Expected int, not " + tok);
    }
  }

  @Override
  public long readLong() throws IOException {
    parser.advance(Symbol.LONG);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      long result = CharSequences.parseLong(csvReader.getElement());
      parseNextCsv();
      return result;
    } else {
      throw new AvroTypeException("Expected long, not " + tok);
    }
  }

  @Override
  public float readFloat() throws IOException {
    parser.advance(Symbol.FLOAT);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      float result = Float.parseFloat(csvReader.getElement().toString());
      parseNextCsv();
      return result;
    } else {
      throw new AvroTypeException("Expected float, not " + tok);
    }
  }

  @Override
  public double readDouble() throws IOException {
    parser.advance(Symbol.DOUBLE);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      double result = Double.parseDouble(csvReader.getElement().toString());
      parseNextCsv();
      return result;
    } else {
      throw new AvroTypeException("Expected double, not " + tok);
    }
  }

  @Override
  public Utf8 readString(final Utf8 utf8) throws IOException {
    parser.advance(Symbol.STRING);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      Utf8 result = new Utf8(csvReader.getElement().toString());
      parseNextCsv();
      return result;
    } else {
      throw new AvroTypeException("Expected string, not " + tok);
    }
  }

  @Override
  public String readString() throws IOException {
    parser.advance(Symbol.STRING);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      String result = csvReader.getElement().toString();
      parseNextCsv();
      return result;
    } else {
      throw new AvroTypeException("Expected string, not " + tok);
    }
  }

  @Override
  public void skipString() throws IOException {
    parser.advance(Symbol.STRING);
    CsvReader.TokenType tok = csvReader.current();
    if (tok != CsvReader.TokenType.ELEMENT) {
      throw new AvroTypeException("Expected string, not " + tok);
    }
    parseNextCsv();
  }

  @Override
  public ByteBuffer readBytes(final ByteBuffer bb) throws IOException {
    parser.advance(Symbol.BYTES);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      ByteBuffer result = ByteBuffer.wrap(Base64.decodeBase64(csvReader.getElement()));
      parseNextCsv();
      return result;
    } else {
      throw new AvroTypeException("Expected string, not " + tok);
    }
  }

  @Override
  public void skipBytes() throws IOException {
    parser.advance(Symbol.BYTES);
    CsvReader.TokenType tok = csvReader.current();
    if (tok != CsvReader.TokenType.ELEMENT) {
      parseNextCsv();
      throw new AvroTypeException("Expected bytes, not " + tok);
    }
  }

  @Override
  public void readFixed(final byte[] bytes, final int i, final int nrBytes) throws IOException {
    parser.advance(Symbol.FIXED);
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      CharSequence text = csvReader.getElement();
      int nrB = Base64.decodeInto(text, 0, text.length(), bytes, i);
      if (nrB != nrBytes) {
        throw new AvroTypeException("invalid fixed lenth " + nrB + " into " + Arrays.toString(bytes));
      }
      parseNextCsv();
    } else {
      throw new AvroTypeException("Expected string, not " + tok);
    }
  }

  @Override
  public void skipFixed(final int i) throws IOException {
    parser.advance(Symbol.FIXED);
    CsvReader.TokenType tok = csvReader.current();
    if (tok != CsvReader.TokenType.ELEMENT) {
      throw new AvroTypeException("Expected fixed, not " + tok);
    }
    parseNextCsv();
  }

  @Override
  public int readEnum() throws IOException {
    parser.advance(Symbol.ENUM);
    Symbol.EnumLabelsAction top = (Symbol.EnumLabelsAction) parser.popSymbol();
    CsvReader.TokenType tok = csvReader.current();
    if (tok == CsvReader.TokenType.ELEMENT) {
      String symb = csvReader.getElement().toString();
      int n = top.findLabel(symb);
      if (n >= 0) {
        parseNextCsv();
        return n;
      } else {
        throw new AvroTypeException("Invalid enum symbol " + symb);
      }
    } else {
      throw new AvroTypeException("Expected string, not " + tok);
    }
  }

  @Override
  public long readArrayStart() throws IOException {
    CsvReader.TokenType current = csvReader.current();
    if (current == CsvReader.TokenType.START_DOCUMENT) {
      throw new IllegalStateException("cannot be at the beginning of " + csvReader);
    }
    parser.advance(Symbol.ARRAY_START);
    if (current == CsvReader.TokenType.ELEMENT) {
      return 1L;
    } else {
      return 0L;
    }
  }

  @Override
  public long arrayNext() throws IOException {
    parser.advance(Symbol.ITEM_END);
    CsvReader.TokenType current = csvReader.current();
    if (current == CsvReader.TokenType.START_DOCUMENT) {
      throw new IllegalStateException("cannot be at the beginning of " + csvReader);
    }
    if (current == CsvReader.TokenType.END_ROW) {
      try {
        current = csvReader.next();
      } catch (CsvParseException ex) {
        throw new AvroRuntimeException(ex);
      }
    }
    if (current == CsvReader.TokenType.ELEMENT) {
      return 1L;
    } else {
      return 0L;
    }
  }

  @Override
  public long skipArray() throws IOException {
    long i = 0;
    while (csvReader.current() != CsvReader.TokenType.END_DOCUMENT) {
      if (csvReader.current() == CsvReader.TokenType.END_ROW) {
        i++;
      }
      try {
        csvReader.next();
      } catch (CsvParseException ex) {
        throw new AvroRuntimeException(ex);
      }
    }
    parser.advance(Symbol.ARRAY_START);
    parser.advance(Symbol.ARRAY_END);
    return i;
  }

  @Override
  public long readMapStart() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long mapNext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long skipMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int readIndex() throws IOException {
    parser.advance(Symbol.UNION);
    Symbol.Alternative a = (Symbol.Alternative) parser.popSymbol();
    String label = csvReader.getElement().toString();
    parser.pushSymbol(Symbol.UNION_END);
    int n = a.findLabel(label);
    if (n < 0) {
      throw new AvroTypeException("Unknown union branch " + label);
    }
    parser.pushSymbol(a.getSymbol(n));
    parseNextCsv();
    return n;
  }

  @Override
  protected void skipFixed() throws IOException {
    parser.advance(Symbol.FIXED);
    CsvReader.TokenType tok = csvReader.current();
    if (tok != CsvReader.TokenType.ELEMENT) {
      throw new AvroTypeException("Expected fixed, not " + tok);
    }
    parseNextCsv();
  }

  @Override
  @Nullable
  public Symbol doAction(final Symbol symbol, final Symbol symbol1) {
    return null;
  }

  @Override
  public String toString() {
    return "CsvDecoder{" + "csvReader=" + csvReader + '}';
  }

  /**
   * Will try to decode the writer schema based on the csv headers, and the reader schema.
   * @param is
   * @param readerSchema
   * @return
   * @throws IOException
   */
  public static DecodedSchema tryDecodeSchema(final InputStream is, @Nullable final Schema readerSchema)
          throws IOException {
    try {
      CsvReader reader = Csv.CSV.reader(new InputStreamReader(is, StandardCharsets.UTF_8));
      Schema record;
      if (readerSchema == null) {
        record = Schema.createRecord("DynCsv", "Infered schema", "org.spf4j.avro", false);
        List<Schema.Field> bfields = new ArrayList<>();
        reader.readRow((cs) -> {
          bfields.add(AvroCompatUtils.createField(cs.toString(), Schema.create(Schema.Type.STRING),
                  null, null, false, false, Schema.Field.Order.IGNORE));
        });
        record.setFields(bfields);
      } else {
        Schema elementType = readerSchema.getElementType();
        List<CharSequence> list = new ArrayList<>(elementType.getFields().size());
        reader.readRow((cs) -> list.add(cs.toString()));
        record = Schemas.project(elementType, list);
      }
      Schema arraySchema = Schema.createArray(record);
      return new DecodedSchema(arraySchema, new CsvDecoder(reader, arraySchema));
    } catch (CsvParseException ex) {
      throw new RuntimeException(ex);
    }
  }




}
