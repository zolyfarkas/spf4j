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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.parsing.JsonGrammarGenerator;
import org.apache.avro.io.parsing.Parser;
import org.apache.avro.io.parsing.Symbol;
import org.apache.avro.util.Utf8;
import org.spf4j.base.Base64;
import org.spf4j.io.csv.CsvWriter;

/**
 * A Csv decoder for avro records.
 *
 * @author Zoltan Farkas
 */
public final class CsvEncoder extends Encoder implements Parser.ActionHandler {

  private final CsvWriter csvWriter;

  private final Parser parser;

  private final Schema writerSchema;

  private boolean firstItem = true;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public CsvEncoder(final CsvWriter csvWriter, final Schema writerSchema) throws IOException {
    this.csvWriter = csvWriter;
    this.parser = new Parser(new JsonGrammarGenerator().generate(writerSchema), this);
    if (writerSchema.getType() != Schema.Type.ARRAY) {
      throw new IllegalArgumentException("schema must be an array: " + writerSchema);
    }
    if (writerSchema.getElementType().getType() != Schema.Type.RECORD) {
      throw new IllegalArgumentException("schema  array element must be record: " + writerSchema);
    }
    this.writerSchema = writerSchema;
  }

  public void writeHeader() throws IOException {
    writeHeader("", writerSchema.getElementType());
    csvWriter.writeEol();
  }

  private void writeHeader(final String prefix, final Schema rowSchema) throws IOException {
    for (Schema.Field field : rowSchema.getFields()) {
      Schema schema = field.schema();
      Schema.Type type = schema.getType();
      String name = prefix + field.name();
      switch (type) {
        case ARRAY:
        case MAP:
          throw new UnsupportedOperationException(type + " unsupported in CSV row" + rowSchema);
        case UNION:
          csvWriter.writeElement(name + ".type");
          for (Schema us : schema.getTypes()) {
            switch (us.getType()) {
              case ARRAY:
              case RECORD:
              case MAP:
                throw new UnsupportedOperationException(type + " unsupported in CSV row union" + rowSchema);
              default:
                continue;
            }
          }
          csvWriter.writeElement(name);
          break;
        case RECORD:
          writeHeader(name + '.', schema);
          break;
        default:
          csvWriter.writeElement(name);
      }
    }
  }

  @Override
  public void writeNull() throws IOException {
    parser.advance(Symbol.NULL);
    csvWriter.writeElement("");
  }

  @Override
  public void writeBoolean(final boolean bln) throws IOException {
    parser.advance(Symbol.BOOLEAN);
    csvWriter.writeElement(Boolean.toString(bln));
  }

  @Override
  public void writeInt(final int i) throws IOException {
    parser.advance(Symbol.INT);
    csvWriter.writeElement(Integer.toString(i));
  }

  @Override
  public void writeLong(final long l) throws IOException {
    parser.advance(Symbol.LONG);
    csvWriter.writeElement(Long.toString(l));
  }

  @Override
  public void writeFloat(final float f) throws IOException {
    parser.advance(Symbol.FLOAT);
    csvWriter.writeElement(Float.toString(f));
  }

  @Override
  public void writeDouble(final double d) throws IOException {
    parser.advance(Symbol.DOUBLE);
    csvWriter.writeElement(Double.toString(d));
  }

  @Override
  public void writeString(final Utf8 utf8) throws IOException {
    parser.advance(Symbol.STRING);
    csvWriter.writeElement(utf8);
  }

  @Override
  public void writeBytes(final ByteBuffer bb) throws IOException {
    parser.advance(Symbol.BYTES);
    int position = bb.position();
    csvWriter.writeElement(Base64.encodeBase64(bb.array(), bb.arrayOffset() + position, bb.limit() - position));
  }

  @Override
  public void writeBytes(final byte[] bytes, final int i, final int length) throws IOException {
    parser.advance(Symbol.BYTES);
    csvWriter.writeElement(Base64.encodeBase64(bytes, i, length));
  }

  @Override
  public void writeFixed(final byte[] bytes, final int i, final int length) throws IOException {
    parser.advance(Symbol.FIXED);
    csvWriter.writeElement(Base64.encodeBase64(bytes, i, length));
  }

  @Override
  public void writeEnum(final int e) throws IOException {
    parser.advance(Symbol.ENUM);
    Symbol.EnumLabelsAction top = (Symbol.EnumLabelsAction) parser.popSymbol();
    csvWriter.writeElement(top.getLabel(e));
  }

  @Override
  public void writeArrayStart() throws IOException {
    firstItem = true;
    parser.advance(Symbol.ARRAY_START);
    // do nothing.
  }

  @Override
  public void writeArrayEnd() throws IOException {
    parser.advance(Symbol.ITEM_END);
    parser.advance(Symbol.ARRAY_END);
    // do nothing.
  }

  @Override
  public void writeMapStart() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeMapEnd() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeIndex(final int unionIndex) throws IOException {
    parser.advance(Symbol.UNION);
    Symbol.Alternative top = (Symbol.Alternative) parser.popSymbol();
    Symbol symbol = top.getSymbol(unionIndex);
    csvWriter.writeElement(top.getLabel(unionIndex));
    parser.pushSymbol(Symbol.UNION_END);
    parser.pushSymbol(symbol);
  }

  @Override
  public void flush() throws IOException {
    csvWriter.flush();
  }

  @Override
  @Nullable
  public Symbol doAction(final Symbol symbol, final Symbol symbol1) {
    return null;
  }

  @Override
  public void setItemCount(final long l) {
    // nothing to do.
  }

  @Override
  public void startItem() throws IOException {
    if (!firstItem) {
      parser.advance(Symbol.ITEM_END);
      csvWriter.writeEol();
    } else {
      firstItem = false;
    }
    // nothing to do
  }

  @Override
  public String toString() {
    return "CsvEncoder{" + "csvWriter=" + csvWriter + ", parser=" + parser
            + ", writerSchema=" + writerSchema + ", firstItem=" + firstItem + '}';
  }

}
