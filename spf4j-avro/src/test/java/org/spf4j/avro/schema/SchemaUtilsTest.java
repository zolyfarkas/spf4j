/*
 * Copyright 2017 SPF4J.
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
package org.spf4j.avro.schema;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.compiler.idl.ParseException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class SchemaUtilsTest {

  @Test
  public void testIdlGenerator() throws IOException, ParseException {
    StringWriter idlStr = new StringWriter();
    Schema rs = SchemaBuilder.builder().record("TestRecord2")
            .namespace("test")
            .doc("record doc")
            .aliases("recordAlias")
            .prop("propKey", "propVal")
            .fields()
            .requiredBoolean("isThere")
            .nullableString("strField", null)
            .name("customField")
            .orderAscending()
            .aliases("blaAlias")
            .prop("fprop", "fval")
            .type(Schema.createArray(Schema.createEnum("someEnum", "enum doc", "test", Arrays.asList("A", "B"))))
            .withDefault(Collections.EMPTY_LIST)
            .name("fixedField")
            .doc("fixedField doc")
            .type(Schema.createFixed("Myfixed", "fixed doc", "test2", 16))
            .noDefault()
            .endRecord();

    SchemaUtils.writeIdlProtocol("TestProtocol", "test", idlStr, rs);
    System.out.println(idlStr);
    Assert.assertFalse(idlStr.toString().isEmpty());
//    Idl idl = new Idl(new StringReader(idlStr.toString()));
//    Protocol protocol = idl.CompilationUnit();
//    System.out.println(protocol);
  }

}
