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
package org.spf4j.avro.official;

import java.util.List;
import org.apache.avro.Schema;
import org.spf4j.avro.AvroCompatUtils;

/**
 * Adapter for the official library.
 * @author Zoltan Farkas
 */
public final class OriginUtilInterface implements AvroCompatUtils.UtilInterface {

  @Override
  public Schema.Field createField(final String name, final Schema schema, final String doc,
          final Object defaultVal,
          final boolean validateDefault, final boolean validateName, final Schema.Field.Order order) {
    return new Schema.Field(name, schema, doc, defaultVal);
  }

  @Override
  public Schema createRecordSchema(final String name, final String doc, final String namespace,
          final boolean isError, final List<Schema.Field> fields, final boolean validateName) {
    return Schema.createRecord(name, doc, namespace, isError, fields);
  }

  @Override
  public Schema createRecordSchema(final String name,
          final String doc, final String namespace, final boolean isError, final boolean validateName) {
    return Schema.createRecord(name, doc, namespace, isError);
  }

}
