/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.maven.plugin.avro.avscp.validation.impl;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class SchemaCompatibilityValidatorTest {

  @Test
  public void testDiff() {

    Schema rec1 = SchemaBuilder.record("test")
            .fields()
            .nullableLong("testField", 0).endRecord();

    Schema rec2 = SchemaBuilder.record("test")
            .fields()
            .nullableInt("testField", 0).endRecord();
    String diff = SchemaCompatibilityValidator.diff(rec1, rec2);

//    System.out.println(SchemaCompatibilityValidator.diff(rec1, rec2));
    Assert.assertThat(diff, Matchers.containsString("int"));
    Assert.assertThat(diff, Matchers.containsString("long"));

  }

}
