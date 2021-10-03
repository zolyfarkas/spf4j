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
import java.util.HashMap;
import javax.annotation.Nonnull;
import org.apache.avro.Schema;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;

/**
 * Implementation that will scan a stream of avro objects. will need to review if QueriableTable is something that fits
 * the bill. https://www.programcreek.com/java-api-examples/?code=vlsi/mat-calcite-plugin/
 * mat-calcite-plugin-master/MatCalcitePlugin/src/com/github/vlsi/mat/calcite/functions/TableFunctions.java
 * @author Zoltan Farkas
 */
abstract class AbstractAvroTable  implements Table  {

  private final org.apache.avro.Schema componentType;

  AbstractAvroTable(final Schema componentType) {
    if (componentType.getType() != org.apache.avro.Schema.Type.RECORD) {
      throw new IllegalArgumentException("Invalid table compoent " + componentType);
    }
    this.componentType = componentType;
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  public final RelDataType getRowType(final RelDataTypeFactory typeFactory) {
    return Types.from((JavaTypeFactory) typeFactory, componentType, new HashMap<>());
  }


  /**
   * Overwrite for better statistics.
   * @return
   */
  @Override
  @Nonnull
  public Statistic getStatistic() {
    return Statistics.UNKNOWN;
  }

  @Override
  public final org.apache.calcite.schema.Schema.TableType getJdbcTableType() {
    return org.apache.calcite.schema.Schema.TableType.TABLE;
  }

  @Override
  public final boolean isRolledUp(final String column) {
    return false;
  }

  @Override
  public final boolean rolledUpColumnValidInsideAgg(final String column, final SqlCall call,
          final SqlNode parent, final CalciteConnectionConfig config) {
    return true;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public final Schema getComponentType() {
    return componentType;
  }

}
