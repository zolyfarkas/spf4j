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
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.PackageInfo;
import org.spf4j.io.csv.CharSeparatedValues;
import org.spf4j.io.csv.CsvParseException;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
public class CsvEncoderTest {

  private static final Logger LOG = LoggerFactory.getLogger(CsvEncoderTest.class);

  @Test
  public void testCsvEncoder() throws IOException, CsvParseException {
    Schema rowschema = SchemaBuilder.record("CsvRow").fields()
            .requiredBoolean("boolField")
            .requiredBytes("bytesField")
            .requiredInt("intField")
            .requiredLong("longField")
            .requiredString("stringField")
            .requiredDouble("doubleField")
            .name("recordField").type(PackageInfo.getClassSchema()).noDefault()
            .nullableString("nullableString", null)
            .name("enumField").type(Schema.createEnum("myEnum", "", "", Arrays.asList("e1", "e2"))).noDefault()
            .endRecord();

    List<GenericRecord> list = testArray(rowschema);

    Schema csvSchema = Schema.createArray(rowschema);

    CharSeparatedValues csv = new CharSeparatedValues(',');
    StringWriter writeTo = new StringWriter();

    CsvEncoder encoder = new CsvEncoder(csv.writer(writeTo), csvSchema);
    encoder.writeHeader();
    DatumWriter writer = new GenericDatumWriter(csvSchema);
    writer.write(list, encoder);
    encoder.flush();
    LOG.debug("written:", writeTo);
    StringReader reader = new StringReader(writeTo.toString());
    CsvDecoder decoder = new CsvDecoder(csv.reader(reader), csvSchema);
    decoder.skipHeader();
    GenericDatumReader dr = new GenericDatumReader(csvSchema);
    Object parsed = dr.read(null, decoder);
    LOG.debug("parser:", parsed);
  }

  private static List<GenericRecord> testArray(final Schema rowschema) {
    Schema enumSchema = rowschema.getField("enumField").schema();
    GenericRecord rec1 = new GenericData.Record(rowschema);
    rec1.put("boolField", Boolean.TRUE);
    rec1.put("bytesField", ByteBuffer.wrap(new byte[] {1, 2, 3}));
    rec1.put("intField", 2);
    rec1.put("longField", 5L);
    rec1.put("stringField", "test1");
    rec1.put("doubleField", Math.PI);
    rec1.put("recordField", new PackageInfo("a", "b"));
    rec1.put("enumField", new GenericData.EnumSymbol(enumSchema, "e1"));
    GenericRecord rec2 = new GenericData.Record(rowschema);
    rec2.put("boolField", Boolean.FALSE);
    rec2.put("bytesField", ByteBuffer.wrap(new byte[] {1, 2, 3}));
    rec2.put("intField", 2);
    rec2.put("longField", 5L);
    rec2.put("stringField", "test1");
    rec2.put("doubleField", Math.PI);
    rec2.put("recordField", new PackageInfo("c", "d"));
    rec2.put("nullableString", "nstr");
    rec2.put("enumField", new GenericData.EnumSymbol(enumSchema, "e2"));
    return Arrays.asList(rec1, rec2);
  }

}
