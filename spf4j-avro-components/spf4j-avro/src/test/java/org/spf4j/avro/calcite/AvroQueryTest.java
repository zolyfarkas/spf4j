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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.calcite.config.Lex;
import org.apache.calcite.interpreter.Interpreter;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.rules.FilterJoinRule;
import org.apache.calcite.rel.rules.ProjectJoinTransposeRule;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.GenericRecordBuilder;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.CloseableIterator;
import org.spf4j.log.Level;
import org.spf4j.test.log.annotations.PrintLogs;

/**
 *
 * @author Zoltan Farkas
 */
public class AvroQueryTest {

  private static final Logger LOG = LoggerFactory.getLogger(AvroQueryTest.class);

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
    SqlParser parser = SqlParser.create("a = b and c = d", cfg);
    SqlNode expr = parser.parseExpression();
    LOG.debug("expression", expr);
    Assert.assertNotNull(expr);
  }


  @Test
  public void testSqlProjection() throws SqlParseException {
    SqlParser.Config cfg = SqlParser.configBuilder()
            .setCaseSensitive(true)
            .setIdentifierMaxLength(255)
            .setLex(Lex.JAVA).build();
    SqlParser parser = SqlParser.create("select a.b as custom, c, d, e", cfg);
    SqlNode expr = parser.parseQuery();
    LOG.debug("expression", expr);
    Assert.assertNotNull(expr);
  }




  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  @PrintLogs(category = "org.codehaus.janino", ideMinLevel = Level.INFO, greedy = true)
  public void testAvroSql() throws SqlParseException, RelConversionException,
          ValidationException, InstantiationException, IllegalAccessException {
    Schema recASchema = SchemaBuilder.record("RecordA")
            .fields().name("id").type().intType().noDefault()
            .requiredString("name").endRecord();
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

    GenericRecordBuilder rb = new GenericRecordBuilder(recASchema, subRecSchema, recBSchema);
    Class<? extends SpecificRecordBase> raC = rb.getRecordClass(recASchema);
    Class<? extends SpecificRecordBase> rbC = rb.getRecordClass(recBSchema);
    Class<? extends SpecificRecordBase> rsC = rb.getRecordClass(subRecSchema);


    GenericRecord reca1 = raC.newInstance();
    reca1.put("id", 1);
    reca1.put("name", "Jim");


    GenericRecord subRec = rsC.newInstance();
    subRec.put("key", "key1");
    subRec.put("value", "val1");


    GenericRecord recb1 = rbC.newInstance();
    recb1.put("id", 1);
    recb1.put("name", "Beam");
    recb1.put("text", "bla");
    recb1.put("adate", LocalDate.now());
    recb1.put("meta", Collections.singletonList(subRec));
    recb1.put("meta2", subRec);

    GenericRecord recb2 = rbC.newInstance();
    recb2.put("id", 2);
    recb2.put("name", "Xi");
    recb2.put("text", "blabla");
    recb2.put("adate", LocalDate.now());
    recb2.put("meta2", subRec);


    SchemaPlus schema = Frameworks.createRootSchema(true);
    schema.add("a", new AvroProjectableFilterableTable(recASchema,
            () -> CloseableIterator.from(Collections.singletonList(reca1).iterator())));
    schema.add("b", new AvroProjectableFilterableTable(recBSchema,
            () -> CloseableIterator.from(Arrays.asList(recb1, recb2).iterator())));

    SqlParser.Config cfg = SqlParser.configBuilder()
            .setCaseSensitive(true)
            .setIdentifierMaxLength(255)
            .setLex(Lex.JAVA).build();
    FrameworkConfig config = Frameworks.newConfigBuilder()
            .parserConfig(cfg)
            .defaultSchema(schema).build();
    Planner planner = Frameworks.getPlanner(config);
    SqlNode s = planner.parse("select a.id, a.name as n1, b.name as n2,"
            + " b.adate as adate, b.meta as firstKey, b.meta2.key as blaKey"
            + " from a"
            + " inner join b on a.id = b.id where b.text like 'bla%' or b.text like 'cucu%'");
    SqlNode validated = planner.validate(s);
    RelRoot rel = planner.rel(validated);
    RelNode plan = rel.project();
    LOG.debug("exec plan", RelOptUtil.toString(plan));
    plan = optimize(plan);
    LOG.debug("exec plan optimized", RelOptUtil.toString(plan));
    RelDataType rowType = plan.getRowType();
    LOG.debug("Return row type: {}", rowType);
    Schema from = Types.from(rowType);
    LOG.debug("Return row schema: {}", from);
    Interpreter interpreter = new Interpreter(new EmbededDataContext(new JavaTypeFactoryImpl()), plan);
    boolean empty = true;
    for (Object[] row : interpreter) {
      GenericRecord record = IndexedRecords.from(from, row);
      LOG.debug("RawRow",  row);
      LOG.debug("Row",  record);
      empty = false;
    }
    Assert.assertFalse(empty);

  }

  private static RelNode optimize(final RelNode rootRel) {
    final HepProgram hepProgram = new HepProgramBuilder()
        //push down predicates
        .addRuleInstance(FilterJoinRule.FilterIntoJoinRule.FILTER_ON_JOIN)
        //push down projections
        .addRuleInstance(ProjectJoinTransposeRule.INSTANCE)
            .build();
    final HepPlanner planner = new HepPlanner(hepProgram);
    planner.setRoot(rootRel);
    return planner.findBestExp();
  }
}
