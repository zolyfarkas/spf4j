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
package org.spf4j.io.avro;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.Test;

/**
 * @author zoly
 */
public class AvroFileReaderTest {


  @Test
  public void testWriteRead() throws IOException {
    Schema schema = SchemaBuilder.builder().record("TestRecord").fields()
            .requiredDouble("number")
            .requiredString("someString").endRecord();
    File file = File.createTempFile("temp", ".tavro");

    AvroFileWriter<GenericRecord> writer = new AvroFileWriter<>(file, schema, GenericRecord.class, 10,
            "test records", true);
    GenericData.Record record = new GenericData.Record(schema);
    record.put("number", 3.14);
    record.put("someString", "test string");
    writer.write(record);
    GenericRecord record2 = new GenericData.Record(record, true);
    record2.put("number", 1);
    writer.write(record2);
    writer.close();

    AvroFileReader<GenericRecord> reader = new AvroFileReader<>(file, schema, GenericRecord.class, 10000);
    GenericRecord read = reader.read();
    Assert.assertEquals(3.14, (double) read.get("number"), 0.0001);
    GenericRecord read2 = reader.read();
    Assert.assertEquals(1, (double) read2.get("number"), 0.0001);
    reader.close();

  }

}
