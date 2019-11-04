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
package org.spf4j.avro.calcite;

import java.time.LocalDate;
import java.util.Collections;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.spf4j.avro.schema.Schemas;

/**
 *
 * @author Zoltan Farkas
 */
public class FilterUtilsTest {


  @Test
  public void testzfilterRecord() throws SqlParseException, ValidationException, RelConversionException {
    Schema subRecSchema = SchemaBuilder.record("SubRecord")
            .fields().name("key").type().stringType().noDefault()
            .requiredString("value").endRecord();
    Schema recBSchema = SchemaBuilder.record("RecordB")
            .fields().name("id").type().intType().noDefault()
            .requiredString("name")
            .requiredString("text")
            .name("adate").type(Schemas.dateString()).noDefault()
            .name("meta").type(Schema.createArray(subRecSchema)).noDefault()
            .name("meta2").type(subRecSchema).noDefault()
            .endRecord();

    GenericRecord subRec = new GenericData.Record(subRecSchema);
    subRec.put("key", "key1");
    subRec.put("value", "val1");


    GenericRecord recb1 =  new GenericData.Record(recBSchema);
    recb1.put("id", 1);
    recb1.put("name", "Beam");
    recb1.put("text", "bla");
    recb1.put("adate", LocalDate.now());
    recb1.put("meta", Collections.singletonList(subRec));
    recb1.put("meta2", subRec);

    Predicate<IndexedRecord> p = FilterUtils.toPredicate("name = 'Beam'", recBSchema);
    assertTrue(p.test(recb1));

    Predicate<IndexedRecord> p2 = FilterUtils.toPredicate("name is null or text like '%bla%'", recBSchema);
    assertTrue(p2.test(recb1));


    Predicate<IndexedRecord> p3 = FilterUtils.toPredicate("name is null", recBSchema);
    assertFalse(p3.test(recb1));


  }

}
