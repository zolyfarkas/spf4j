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
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.spf4j.avro.SqlPredicate;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("STT_TOSTRING_STORED_IN_FIELD") // kind of valid, will deal with it later
public final class SqlRowPredicate<T extends IndexedRecord> implements SqlPredicate<T> {

  private final String sqlExpr;

  private final Predicate<IndexedRecord> pred;

  public SqlRowPredicate(final String sqlExpr, final Schema rowSchema)
          throws SqlParseException, ValidationException, RelConversionException {
    this.sqlExpr = sqlExpr.trim();
    pred = FilterUtils.toPredicate(this.sqlExpr, rowSchema);
  }

  public SqlRowPredicate(final List<RexNode> filter, final RelDataType rowType) {
    if (filter.isEmpty()) {
      this.sqlExpr = "";
      pred = (x) -> true;
    } else {
      List<SqlNode> convert = SqlConverters.convert(filter, rowType);
      if (convert.size() == 1) {
        this.sqlExpr = convert.get(0).toString();
      } else {
        StringBuilder sqlB = new StringBuilder();
        Iterator<SqlNode> iterator = convert.iterator();
        sqlB.append('(');
        sqlB.append(iterator.next());
        sqlB.append(')');
        while (iterator.hasNext()) {
          sqlB.append(" AND (");
          sqlB.append(iterator.next());
          sqlB.append(')');
        }
        this.sqlExpr = sqlB.toString();
      }
      pred = FilterUtils.toPredicate(filter, rowType);
    }
  }

  @Override
  public boolean test(final IndexedRecord t) {
    return pred.test(t);
  }

  @Override
  public String toString() {
    return sqlExpr;
  }

  @Override
  public String getSqlString() {
    return sqlExpr;
  }

}
