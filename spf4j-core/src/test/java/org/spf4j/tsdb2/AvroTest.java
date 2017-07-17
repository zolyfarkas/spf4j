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
package org.spf4j.tsdb2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import org.apache.avro.Schema;
import org.junit.Assert;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.Test;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.tsdb2.avro.DataBlock;

/**
 *
 * @author zoly
 */
public class AvroTest {

  @Test
  public void testRw() throws IOException {
    DataBlock data = DataBlock.newBuilder()
            .setBaseTimestamp(0).setValues(Collections.EMPTY_LIST).build();
    try (ByteArrayBuilder bab = new ByteArrayBuilder()) {
      Schema schema = data.getSchema();
      SpecificDatumWriter<DataBlock> writer = new SpecificDatumWriter<>(schema);
      final BinaryEncoder directBinaryEncoder = EncoderFactory.get().directBinaryEncoder(bab, null);
      writer.write(data, directBinaryEncoder);
      directBinaryEncoder.flush();
      ByteArrayInputStream bis = new ByteArrayInputStream(bab.getBuffer(), 0, bab.size());
      SpecificDatumReader<DataBlock> reader = new SpecificDatumReader<>(schema);
      BinaryDecoder directBinaryDecoder = DecoderFactory.get().directBinaryDecoder(bis, null);
      DataBlock read = reader.read(null, directBinaryDecoder);
      Assert.assertEquals(read, data);
    }
  }

}
