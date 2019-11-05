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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.config.Lex;
import org.apache.calcite.interpreter.Scalar;
import org.apache.calcite.interpreter.Spf4jDataContext;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.spf4j.base.CloseableIterator;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class FilterUtils {

  private FilterUtils() { }

  private static final SqlParser.Config PARSER_CFG = SqlParser.configBuilder()
            .setCaseSensitive(true)
            .setIdentifierMaxLength(255)
            .setLex(Lex.JAVA).build();

  private static final JavaTypeFactoryImpl JAVA_TYPE_FACTORY = new JavaTypeFactoryImpl();

  public static SqlNode parse(final String expr) throws SqlParseException {
    SqlParser parser = SqlParser.create(expr, PARSER_CFG);
    return parser.parseExpression();
  }


  public static Predicate<IndexedRecord> toPredicate(final String sqlExpr,
          final Schema recSchema) throws SqlParseException,
    ValidationException, RelConversionException {
    return toPredicate(sqlExpr, JAVA_TYPE_FACTORY, recSchema);
  }

  public static Predicate<IndexedRecord> toPredicate(final String sqlExpr,
          final JavaTypeFactoryImpl javaTypeFactoryImpl,
          final Schema recSchema) throws SqlParseException,
          ValidationException, RelConversionException {
    SchemaPlus schema = Frameworks.createRootSchema(true);
    schema.add("r", new AvroProjectableFilterableTable(recSchema,
            () -> CloseableIterator.from(Collections.EMPTY_LIST.iterator())));
    FrameworkConfig config = Frameworks.newConfigBuilder()
            .parserConfig(PARSER_CFG)
            .defaultSchema(schema).build();

    Planner planner = Frameworks.getPlanner(config);
    SqlNode parse = planner.parse("select * from r where " + sqlExpr);
    parse = planner.validate(parse);
    RelNode project = planner.rel(parse).project();
    List<RexNode> childExps = ((LogicalFilter) project.getInput(0)).getChildExps();
    RelDataType from = Types.from(javaTypeFactoryImpl, recSchema, new HashMap<Schema, RelDataType>());
    return toPredicate(childExps, javaTypeFactoryImpl, from);
  }

  public static Predicate<IndexedRecord> toPredicate(final List<RexNode> filter, final RelDataType rowType) {
    return toPredicate(filter, JAVA_TYPE_FACTORY, rowType);
  }


  public static Predicate<IndexedRecord> toPredicate(final List<RexNode> filter,
          final JavaTypeFactoryImpl javaTypeFactoryImpl, final RelDataType rowType) {
    Scalar scalar = InterpreterUtils.toScalar(filter, javaTypeFactoryImpl, rowType);
    return new Predicate<IndexedRecord>() {

      private Spf4jDataContext context = new Spf4jDataContext(new EmbededDataContext(javaTypeFactoryImpl));
      {
        context.values =  new Object[rowType.getFieldCount()];
      }

      @Override
      public synchronized boolean test(final IndexedRecord x) {
        IndexedRecords.copyRecord(x, context.values);
        return (Boolean) scalar.execute(context);
      }
    };
  }

  public static Predicate<IndexedRecord> toPredicate(@Nullable final Iterable<String> expr,
          final Schema recSchema) throws SqlParseException, ValidationException, RelConversionException {
    if (expr == null) {
      return (x) -> true;
    }
    Iterator<String> it = expr.iterator();
    if (!it.hasNext()) {
      return (x) -> true;
    }
    Predicate<IndexedRecord> result = toPredicate(it.next(), recSchema);
    while (it.hasNext()) {
      result = result.and(toPredicate(it.next(), recSchema));
    }
    return result;
  }


}
