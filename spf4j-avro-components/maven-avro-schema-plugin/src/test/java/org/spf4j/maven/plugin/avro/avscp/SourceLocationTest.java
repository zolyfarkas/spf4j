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
package org.spf4j.maven.plugin.avro.avscp;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class SourceLocationTest {



  @Test
  public void testSourceLocation() {
    SourceLocation sl = new SourceLocation("src/main/avro/txn/BlBla.avdl:6:5");
    Assert.assertEquals(6, sl.getLineNr());
    Assert.assertEquals(5, sl.getColNr());
    Assert.assertEquals("src/main/avro/txn/BlBla.avdl", sl.getFilePath());
  }

}
