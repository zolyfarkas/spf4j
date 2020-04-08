package org.apache.avro;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/*
 * Copyright 2020 SPF4J.
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

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({ "SE_BAD_FIELD", "SE_NO_SERIALVERSIONID" })
public final class ExtendedAvroNamesRefResolver extends ExtendedNames {

  private final SchemaResolver sResolver;

  public ExtendedAvroNamesRefResolver(final SchemaResolver sResolver) {
    this.sResolver = sResolver;
  }

  public ExtendedAvroNamesRefResolver(final SchemaResolver sClient, String space) {
    super(space);
    this.sResolver = sClient;
  }

  public String getId(Schema schema) {
    return sResolver.getId(schema);
  }

  public Schema resolveSchema(String id) {
    return sResolver.resolveSchema(id);
  }

  @Override
  public String toString() {
    return "AvroNamesRefResolver{" + "sResolver=" + sResolver + '}';
  }

}
