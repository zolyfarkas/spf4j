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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSqlStandardConvertletTable;
import org.apache.calcite.rex.RexToSqlNodeConverterImpl;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParserPos;

public final class SqlConverters {

  private SqlConverters() { }

  private static final RexSqlStandardConvertletTable TABLE = new RexSqlStandardConvertletTable();

  @Nullable
  public static List<String> projectionToString(@Nullable final int[] projection, final RelDataType rowType) {
    if (projection == null) {
      return null;
    }
    List<String> result = new ArrayList<>(projection.length);
    List<String> fieldNames = rowType.getFieldNames();
    for (int i = 0; i < projection.length; i++) {
      result.add(fieldNames.get(projection[i]));
    }
    return result;
  }

  @Nullable
  public static SqlNode convert(final RexNode node, final RelDataType rowType) {
   RexToSqlNodeConverterImpl conv = new RexToSqlNodeConverterImpl(TABLE) {
     @Override
     public SqlNode convertInputRef(final RexInputRef ref) {
       return new SqlIdentifier(rowType.getFieldNames().get(ref.getIndex()), SqlParserPos.ZERO);
     }
   };
   return conv.convertNode(node);
  }

  public static List<SqlNode> convert(final List<RexNode> filters, final RelDataType rowType) {
    List<SqlNode> result = new ArrayList<>(filters.size());
    for (RexNode node : filters) {
      result.add(convert(node, rowType));
    }
    return result;
  }


  @Nullable
  public static SqlNode convert(final RexNode node, final Schema rowSchema) {
   RexToSqlNodeConverterImpl conv = new RexToSqlNodeConverterImpl(TABLE) {
     @Override
     public SqlNode convertInputRef(final RexInputRef ref) {
       return new SqlIdentifier(rowSchema.getFields().get(ref.getIndex()).name(), SqlParserPos.ZERO);
     }

   };
   return conv.convertNode(node);
  }

  public static List<SqlNode> convert(final List<RexNode> filters, final Schema rowSchema) {
    List<SqlNode> result = new ArrayList<>(filters.size());
    for (RexNode node : filters) {
      result.add(convert(node, rowSchema));
    }
    return result;
  }

}
