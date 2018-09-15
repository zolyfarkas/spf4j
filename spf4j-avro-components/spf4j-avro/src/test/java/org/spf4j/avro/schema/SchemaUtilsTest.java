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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class SchemaUtilsTest {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaUtilsTest.class);

  @Test
  public void testIdlGenerator() throws IOException, ParseException {
    StringWriter idlStrWriter = new StringWriter();
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
            .prop("fprp2", "fval2")
            .type(Schema.createArray(Schema.createEnum("someEnum", "enum doc", "test", Arrays.asList("A", "B"))))
            .withDefault(Collections.EMPTY_LIST)
            .name("fixedField")
            .doc("fixedField doc")
            .type(Schema.createFixed("Myfixed", "fixed doc", "test2", 16))
            .noDefault()
            .endRecord();

    SchemaUtils.writeIdlProtocol("TestProtocol", "test", idlStrWriter, rs);
    LOG.debug("writer = {}", idlStrWriter);
    String idlStr = idlStrWriter.toString();
    Assert.assertFalse(idlStr.isEmpty());
    Idl idl = new Idl(new StringReader(idlStr));
    Protocol protocol = idl.CompilationUnit();
    LOG.debug("Protocol = {}", protocol);
    Schema rs2 = protocol.getType("test.TestRecord2");
    Assert.assertEquals(rs, rs2);

  }

  @Test
  public void testIdlGenerator2() throws IOException, ParseException {
    StringWriter idlStrWriter = new StringWriter();
    Schema rs = SchemaBuilder.builder().record("TestRecord2")
            .namespace("test")
            .doc("record doc")
            .aliases("recordAlias")
            .prop("propKey", "propVal")
            .fields()
            .requiredBoolean("isThere")
            .nullableString("strField", null)
            .endRecord();

    SchemaUtils.writeIdlProtocol("TestProtocol", "test", idlStrWriter, rs);
    LOG.debug("writer = {}", idlStrWriter);
    String idlStr = idlStrWriter.toString();
    Assert.assertFalse(idlStr.isEmpty());
    Idl idl = new Idl(new StringReader(idlStr));
    Protocol protocol = idl.CompilationUnit();
    LOG.debug("Protocol = {}", protocol);
    Schema rs2 = protocol.getType("test.TestRecord2");
    Assert.assertEquals(rs, rs2);

  }


}
