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

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.calcite.rel.type.RelDataType;
import java.util.Map;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * @author Zoltan Farkas
 */
public final class Types {

  private Types() { }

  private static final Interner<RelDataType> TI = Interners.newStrongInterner();

  public static RelDataType from(final JavaTypeFactory fact,
          final Schema schema, final Map<Schema, RelDataType> defined) {
    RelDataType ex = defined.get(schema);
    if (ex != null) {
      return ex;
    }
    RelDataType result;
    switch (schema.getType())  {
      case STRING:
        result = TI.intern(fact.createSqlType(SqlTypeName.VARCHAR));
        break;
      case INT:
        result = TI.intern(fact.createSqlType(SqlTypeName.INTEGER));
        break;
      case DOUBLE:
        result = TI.intern(fact.createSqlType(SqlTypeName.DOUBLE));
        break;
      case FLOAT:
        result = TI.intern(fact.createSqlType(SqlTypeName.FLOAT));
        break;
      case LONG:
        result = TI.intern(fact.createSqlType(SqlTypeName.BIGINT));
        break;
      case RECORD:
        result = TI.intern(fact.createStructType(fromRecordSchema(fact, schema)));
        break;
      default:
        throw new UnsupportedOperationException("Unsupported: " + schema);
    }
    defined.put(schema, result);
    return result;
  }

  public static List<RelDataTypeField> fromRecordSchema(final JavaTypeFactory fact, final Schema schema) {
    List<Schema.Field> fields = schema.getFields();
    List<RelDataTypeField> result = new ArrayList<>(fields.size());
    IdentityHashMap<org.apache.avro.Schema, RelDataType> map = new IdentityHashMap<>();
    for (Schema.Field field : fields)   {
      result.add(
            new RelDataTypeFieldImpl(
                field.name(),
                field.pos(),
                Types.from(fact, field.schema(), map)));
    }
    return result;
  }

}
