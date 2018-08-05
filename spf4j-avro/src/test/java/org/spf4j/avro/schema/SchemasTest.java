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
package org.spf4j.avro.schema;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public class SchemasTest {

  private static final Logger LOG = LoggerFactory.getLogger(SchemasTest.class);

  private static final String SCHEMA = "{\"type\":\"record\",\"name\":\"SampleNode\",\"doc\":\"caca\","
          + "\"namespace\":\"org.spf4j.ssdump2.avro\",\n"
          + " \"fields\":[\n"
          + "    {\"name\":\"count\",\"type\":\"int\",\"default\":0,\"doc\":\"caca\"},\n"
          + "    {\"name\":\"subNodes\",\"type\":\n"
          + "       {\"type\":\"array\",\"items\":{\n"
          + "           \"type\":\"record\",\"name\":\"SamplePair\",\n"
          + "           \"fields\":[\n"
          + "              {\"name\":\"method\",\"type\":\n"
          + "                  {\"type\":\"record\",\"name\":\"Method\",\n"
          + "                  \"fields\":[\n"
          + "                     {\"name\":\"declaringClass\","
          + "\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},\n"
          + "                     {\"name\":\"methodName\","
          + "\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}\n"
          + "                  ]}},\n"
          + "              {\"name\":\"node\",\"type\":\"SampleNode\"}]}}}]}";

  @Test
  public void testVisit() throws IOException {
    Schema recSchema = new Schema.Parser().parse(SCHEMA);
    Schemas.visit(recSchema, new PrintingVisitor());

    String schemaStr = Resources.toString(Resources.getResource("SchemaBuilder.avsc"), StandardCharsets.US_ASCII);
    Schema schema = new Schema.Parser().parse(schemaStr);

    Map<String, Schema> schemas = Schemas.visit(schema, new SchemasWithClasses());
    Assert.assertThat(schemas, Matchers.hasValue(schema));

    Schema trimmed = Schemas.visit(recSchema, new CloningVisitor(recSchema));
    Assert.assertNull(trimmed.getDoc());
    Assert.assertNotNull(recSchema.getDoc());

    SchemaCompatibility.SchemaCompatibilityType compat
            = SchemaCompatibility.checkReaderWriterCompatibility(trimmed, recSchema).getType();
    Assert.assertEquals(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE, compat);
    compat = SchemaCompatibility.checkReaderWriterCompatibility(recSchema, trimmed).getType();
    Assert.assertEquals(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE, compat);

    Schema unmodifyable = Schemas.visit(recSchema, new ImmutableCloningVisitor(recSchema, false));
    Assert.assertNotNull(unmodifyable.getDoc());
    compat
            = SchemaCompatibility.checkReaderWriterCompatibility(unmodifyable, recSchema).getType();
    Assert.assertEquals(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE, compat);
    compat = SchemaCompatibility.checkReaderWriterCompatibility(recSchema, unmodifyable).getType();
    Assert.assertEquals(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE, compat);

    Schema schema1 = unmodifyable.getField("subNodes").schema().getElementType().getField("node").schema();
    try {
      schema1.addAlias("yahooo");
      Assert.fail();
    } catch (UnsupportedOperationException ex) {
    }

    try {
      schema1.setFields(Collections.EMPTY_LIST);
      Assert.fail();
    } catch (UnsupportedOperationException ex) {
    }

  }

  private static class PrintingVisitor implements SchemaVisitor {

    @Override
    public SchemaVisitorAction visitTerminal(final Schema terminal) {
      LOG.debug("Terminal: {}", terminal.getFullName());
      return SchemaVisitorAction.CONTINUE;
    }

    @Override
    public SchemaVisitorAction visitNonTerminal(final Schema terminal) {
      LOG.debug("NONTerminal start: {}", terminal.getFullName());
      return SchemaVisitorAction.CONTINUE;
    }

    @Override
    public SchemaVisitorAction afterVisitNonTerminal(final Schema terminal) {
      LOG.debug("NONTerminal end: {}", terminal.getFullName());
      return SchemaVisitorAction.CONTINUE;
    }
  }

}
