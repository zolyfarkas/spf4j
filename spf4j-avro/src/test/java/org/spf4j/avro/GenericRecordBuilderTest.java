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

import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public class GenericRecordBuilderTest {

  private static final Logger LOG = LoggerFactory.getLogger(GenericRecordBuilderTest.class);

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testGenericRecordCreation() throws IOException, Exception {
    String schemaStr = Resources.toString(Resources.getResource("SchemaBuilder.avsc"), StandardCharsets.US_ASCII);
    Schema schema = new Schema.Parser().parse(schemaStr);
    try (GenericRecordBuilder builder = new GenericRecordBuilder(schema)) {
      Class<? extends SpecificRecordBase> clasz = builder.getRecordClass(schema);
      GenericRecord record = clasz.newInstance();
      record.put("requiredBoolean", Boolean.TRUE);
      Schema fieldSchema = schema.getField("optionalRecord").schema().getTypes().get(1);
      Class<? extends SpecificRecordBase> nestedRecordClass = builder.getRecordClass(fieldSchema);
      SpecificRecordBase nr = nestedRecordClass.newInstance();
      nr.put("nestedRequiredBoolean", Boolean.TRUE);
      record.put("optionalRecord", nr);
      LOG.debug("Record = {}", record);
      Assert.assertEquals(nr, record.get("optionalRecord"));
    }
  }

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testMoreGenericRecordCreation() throws InstantiationException, IllegalAccessException {
    Schema reuse = SchemaBuilder.builder().record("Reuse").fields().requiredString("field").endRecord();
    Schema record = SchemaBuilder.builder().record("TestRecord").fields()
            .requiredInt("number")
            .name("record").type(reuse).noDefault()
            .endRecord();
    Schema record2 = SchemaBuilder.builder().record("TestRecord2").fields()
            .requiredBoolean("isThere")
            .name("record").type(reuse).noDefault()
            .endRecord();
    try (GenericRecordBuilder builder = new GenericRecordBuilder(record, record2)) {
      Class<? extends SpecificRecordBase> clasz = builder.getRecordClass(record);
      Class<? extends SpecificRecordBase> clasz2 = builder.getRecordClass(record);
      Assert.assertSame(clasz, clasz2);
      SpecificRecordBase myRecord = clasz.newInstance();
      myRecord.put("number", 35);
      Assert.assertEquals(35, (int) myRecord.get("number"));
      Class<? extends SpecificRecordBase> aClass = builder.getRecordClass(record2);
      GenericRecord r2 = aClass.newInstance();
      LOG.debug("Record = {}", r2);
    }
  }

  @Test
  public void testEnumImplementation() {
    Schema enumSchema = SchemaBuilder.enumeration("MyEnum").namespace("test").symbols("A", "B", "C");
    try (GenericRecordBuilder builder = new GenericRecordBuilder(enumSchema)) {
      Class<? extends GenericEnumSymbol> enumClass = builder.getEnumClass(enumSchema);
      Assert.assertEquals("test.MyEnum", enumClass.getName());
    }
  }

}
