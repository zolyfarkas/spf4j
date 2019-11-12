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
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.calcite.rel.type.RelDataType;
import java.util.Map;
import org.apache.avro.LogicalType;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.spf4j.avro.schema.Schemas;

/**
 *
 * similar is done in drill, seems like calcite issue I have also stumbled upon:
 *
 * https://github.com/apache/drill/blob/master/exec/java-exec/src
 * /main/java/org/apache/drill/exec/store/avro/AvroDrillTable.java
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("CC_CYCLOMATIC_COMPLEXITY")
public final class Types {

  private Types() { }

  public static Schema from(final RelDataType dataType) {
    SqlTypeName sqlTypeName = dataType.getSqlTypeName();
    Schema  result;
    switch (sqlTypeName) {
      case ROW:
       List<RelDataTypeField> fieldList = dataType.getFieldList();
       List<Schema.Field> aFields = new ArrayList<>(fieldList.size());
       for (RelDataTypeField field : fieldList) {
         aFields.add(new Schema.Field(field.getName(), from(field.getType()), null, (Object) null));
       }
       return Schema.createRecord(aFields);
      case INTEGER:
        result = Schema.create(Schema.Type.INT);
        break;
      case BIGINT:
        result = Schema.create(Schema.Type.LONG);
        break;
      case VARCHAR:
        result = Schema.create(Schema.Type.STRING);
        break;
      case DATE:
        result = Schemas.dateString();
        break;
      case BINARY:
        int precision = dataType.getPrecision();
        if (precision > 0) {
          result = Schema.createFixed(null, null, null, precision);
        } else {
          result = Schema.create(Schema.Type.BYTES);
        }
        break;
      case DOUBLE:
      case REAL:
      case DECIMAL:
        result = Schema.create(Schema.Type.DOUBLE);
        break;
          // disabled until: https://issues.apache.org/jira/browse/CALCITE-3494
//        Schema stringSchema = Schema.create(Schema.Type.STRING)
//            .withProp("logicalType", "decimal");
//        int prec = dataType.getPrecision();
//        if (prec != RelDataType.PRECISION_NOT_SPECIFIED) {
//          stringSchema = stringSchema.withProp("precision", prec);
//        }
//        int scale = dataType.getScale();
//        if (scale != RelDataType.SCALE_NOT_SPECIFIED) {
//          stringSchema = stringSchema.withProp("scale", scale);
//        }
//        LogicalType dlt = new DecimalFactory().fromSchema(stringSchema);
//        stringSchema.setLogicalType(dlt);
//        result = stringSchema;
//        break;
      case FLOAT:
        result = Schema.create(Schema.Type.FLOAT);
        break;
      case BOOLEAN:
        result = Schema.create(Schema.Type.BOOLEAN);
        break;
      case ARRAY:
      case MULTISET:
        result = Schema.createArray(from(dataType.getComponentType()));
        break;
      case MAP:
        result = Schema.createMap(from(dataType.getValueType()));
        break;
      default:
        throw new UnsupportedOperationException("Unsupported data Type " + dataType);
    }
    if (dataType.isNullable()) {
      result = Schema.createUnion(Schema.create(Schema.Type.NULL), result);
    }
    return result;
  }


  @SuppressWarnings("checkstyle:MissingSwitchDefault")
  public static RelDataType from(final JavaTypeFactory fact,
          final Schema schema, final Map<Schema, RelDataType> defined) {
    RelDataType ex = defined.get(schema);
    if (ex != null) {
      return ex;
    }
    LogicalType logicalType = schema.getLogicalType();
    if (logicalType != null) {
      switch (logicalType.getName()) {
        case "date":
          return fact.createSqlType(SqlTypeName.DATE);
        case "instant":
          return fact.createSqlType(SqlTypeName.TIMESTAMP);
        case "decimal":
          // disabled until: https://issues.apache.org/jira/browse/CALCITE-3494
//          Number precision = (Number) logicalType.getProperty("precision");
//          Number scale = (Number) logicalType.getProperty("precision");
//          if (scale == null) {
//            if (precision == null) {
//              return fact.createSqlType(SqlTypeName.DECIMAL, 36);
//            } else {
//              return fact.createSqlType(SqlTypeName.DECIMAL, precision.intValue());
//            }
//          } else {
//            if (precision == null) {
//              return fact.createSqlType(SqlTypeName.DECIMAL, 36, scale.intValue());
//            } else {
//              return fact.createSqlType(SqlTypeName.DECIMAL, precision.intValue(), scale.intValue());
//            }
//          }
          return fact.createSqlType(SqlTypeName.DOUBLE);
      }
    }
    RelDataType result;
    switch (schema.getType())  {
      case STRING:
        result = fact.createSqlType(SqlTypeName.VARCHAR);
        break;
      case BOOLEAN:
        result = fact.createSqlType(SqlTypeName.BOOLEAN);
        break;
      case FIXED:
        result = fact.createSqlType(SqlTypeName.BINARY, schema.getFixedSize());
        break;
      case BYTES:
        result = fact.createSqlType(SqlTypeName.BINARY);
        break;
      case INT:
        result = fact.createSqlType(SqlTypeName.INTEGER);
        break;
      case ENUM:
        result = fact.createSqlType(SqlTypeName.SYMBOL);
        break;
      case DOUBLE:
        result = fact.createSqlType(SqlTypeName.DOUBLE);
        break;
      case FLOAT:
        result = fact.createSqlType(SqlTypeName.FLOAT);
        break;
      case LONG:
        result = fact.createSqlType(SqlTypeName.BIGINT);
        break;
      case RECORD:
        result = fact.createStructType(fromRecordSchema(fact, schema));
        break;
      case MAP:
        result = fact.createMapType(fact.createSqlType(SqlTypeName.VARCHAR),
                from(fact, schema.getValueType(), defined));
        break;
      case ARRAY:
        result = fact.createArrayType(from(fact, schema.getElementType(), defined), -1);
        break;
      case UNION:
        Schema nullableUnionSchema = Schemas.nullableUnionSchema(schema);
        if (nullableUnionSchema != null) {
          result = fact.createTypeWithNullability(from(fact, nullableUnionSchema, defined), true);
          break;
        } else {
          throw new UnsupportedOperationException("Unsupported: " + schema);
        }
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
