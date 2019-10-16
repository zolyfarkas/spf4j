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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.generic.GenericData;
import org.apache.calcite.config.Lex;
import org.apache.calcite.interpreter.Interpreter;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.rules.FilterJoinRule.FilterIntoJoinRule;
import org.apache.calcite.rel.rules.FilterProjectTransposeRule;
import org.apache.calcite.rel.rules.FilterTableScanRule;
import org.apache.calcite.rel.rules.ProjectJoinTransposeRule;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.RuleSets;
import org.apache.calcite.tools.ValidationException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.calcite.AvroProjectableFilterableTable;
import org.spf4j.avro.calcite.EmbededDataContext;
import org.spf4j.base.CloseableIterable;
import org.spf4j.demo.avro.DemoRecordInfo;

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

  @Test
  public void testSchemaPath() {
    Schema subSchema = Schemas.getSubSchema(DemoRecordInfo.SCHEMA$, "demoRecord.id");
    Assert.assertEquals(Schema.Type.STRING, subSchema.getType());
  }

  @Test
  public void testSchemaPath2() {
    Schema subSchema = Schemas.getSubSchema(Schema.createArray(DemoRecordInfo.SCHEMA$), "[].demoRecord.id");
    Assert.assertEquals(Schema.Type.STRING, subSchema.getType());
  }

  @Test
  public void testProjections() {
    Schema project = Schemas.project(DemoRecordInfo.SCHEMA$, "demoRecord.id", "metaData");
    Schema.Field drF = project.getField("demoRecord");
    Assert.assertEquals(0, drF.pos());
    List<Schema.Field> drf = drF.schema().getFields();
    Assert.assertEquals(1, drf.size());
    Assert.assertEquals("id", drf.get(0).name());
    Assert.assertEquals(DemoRecordInfo.SCHEMA$.getField("metaData").schema(), project.getField("metaData").schema());
  }

  @Test
  public void testProjections2() {
    Schema project = Schemas.project(DemoRecordInfo.SCHEMA$, "demoRecord.id", "bubu");
    Assert.assertNull(project);
  }

  @Test
  public void testProjectionsOrder() {
    Schema project = Schemas.project(DemoRecordInfo.SCHEMA$, "metaData", "demoRecord.id");
    Schema.Field drF = project.getField("demoRecord");
    Assert.assertEquals(1, drF.pos());
    List<Schema.Field> drf = drF.schema().getFields();
    Assert.assertEquals(1, drf.size());
    Schema.Field field = drf.get(0);
    Assert.assertEquals("id", field.name());
    Assert.assertEquals(DemoRecordInfo.SCHEMA$.getField("metaData").schema(), project.getField("metaData").schema());
  }


  @Test
  public void testSqlParser() throws SqlParseException {
    SqlParser.Config cfg = SqlParser.configBuilder()
            .setCaseSensitive(true)
            .setIdentifierMaxLength(255)
            .setLex(Lex.JAVA).build();
    SqlParser parser = SqlParser.create("select a.id, a.name as n1, b.name as n2 from a, b where  a.id = b.id", cfg);
    SqlSelect select = (SqlSelect) parser.parseQuery();
    LOG.debug("Select", select);
    Assert.assertNotNull(select);
  }

  @Test
  public void testSqlFilter() throws SqlParseException {
    SqlParser.Config cfg = SqlParser.configBuilder()
            .setCaseSensitive(true)
            .setIdentifierMaxLength(255)
            .setLex(Lex.JAVA).build();
    SqlParser parser = SqlParser.create("a = b and c=d", cfg);
    SqlNode expr = parser.parseExpression();
    LOG.debug("expression", expr);
    Assert.assertNotNull(expr);
  }




  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testAvroSql() throws SqlParseException, RelConversionException, ValidationException {
    Schema recASchema = SchemaBuilder.record("RecordA")
            .fields().name("id").type().intType().noDefault()
            .requiredString("name").endRecord();
    Schema recBSchema = SchemaBuilder.record("RecordB")
            .fields().name("id").type().intType().noDefault()
            .requiredString("name")
            .requiredString("text")
            .endRecord();


    GenericData.Record reca1 = new GenericData.Record(recASchema);
    reca1.put("id", 1);
    reca1.put("name", "Jim");

    GenericData.Record recb1 = new GenericData.Record(recBSchema);
    recb1.put("id", 1);
    recb1.put("name", "Beam");
    recb1.put("text", "bla");

    GenericData.Record recb2 = new GenericData.Record(recBSchema);
    recb2.put("id", 2);
    recb2.put("name", "Xi");
    recb2.put("text", "blabla");


    SchemaPlus schema = Frameworks.createRootSchema(true);
    schema.add("a", new AvroProjectableFilterableTable(recASchema,
            () -> CloseableIterable.fromIterable(Collections.singletonList(reca1))));
    schema.add("b", new AvroProjectableFilterableTable(recBSchema,
            () -> CloseableIterable.fromIterable(Arrays.asList(recb1, recb2))));

    SqlParser.Config cfg = SqlParser.configBuilder()
            .setCaseSensitive(true)
            .setIdentifierMaxLength(255)
            .setLex(Lex.JAVA).build();
    FrameworkConfig config = Frameworks.newConfigBuilder()
            .parserConfig(cfg)
            .ruleSets(RuleSets.ofList(FilterTableScanRule.INSTANCE,
                    FilterProjectTransposeRule.INSTANCE, FilterIntoJoinRule.FILTER_ON_JOIN))
            .defaultSchema(schema).build();
    Planner planner = Frameworks.getPlanner(config);
    SqlNode s = planner.parse("select a.id, a.name as n1, b.name as n2 from a"
            + " inner join  b on a.id = b.id where b.text = 'bla'");
    SqlNode validated = planner.validate(s);
    RelRoot rel = planner.rel(validated);
    LOG.debug("exec plan", RelOptUtil.toString(rel.project()));
    RelNode optimized = optimize(rel.project());
    LOG.debug("exec plan optimized", RelOptUtil.toString(optimized));

    Interpreter interpreter = new Interpreter(new EmbededDataContext(planner), optimized);
    for (Object[] row : interpreter) {
      LOG.debug("Row", row);
      Assert.assertNotNull(row);
    }

  }

  private RelNode optimize(final RelNode rootRel) {
    final HepProgram hepProgram = new HepProgramBuilder()
        //push down predicates
        .addRuleInstance(FilterIntoJoinRule.FILTER_ON_JOIN)
        //push down projections
        .addRuleInstance(ProjectJoinTransposeRule.INSTANCE)
            .build();
    final HepPlanner planner = new HepPlanner(hepProgram);
    planner.setRoot(rootRel);
    return planner.findBestExp();
  }

}
