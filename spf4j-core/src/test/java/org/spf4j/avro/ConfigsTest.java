/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
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
package org.spf4j.avro;

import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import org.junit.Test;
import org.spf4j.failsafe.avro.TimeoutRelativeHedgePolicy;
import  org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class ConfigsTest {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigsTest.class);

  private static final String TEST_SCHEMA =
    "{\"type\":\"record\",\"name\":\"TimeoutRelativeHedgePolicy\",\"namespace\":\"org.spf4j.failsafe.avro\","
    + "\"fields\":["
    + "{\"name\":\"minHedgeDelay\",\"type\":{\"type\":\"string\",\"logicalType\":\"duration\"},\"default\":\"PT0S\"},"
    + "{\"name\":\"maxHedgeDelay\",\"type\":{\"type\":\"string\",\"logicalType\":\"duration\"},\"default\":\"PT0S\"},"
    + "{\"name\":\"factor\",\"type\":\"double\",\"default\":0.0},"
    + "{\"name\":\"nrHedges\",\"type\":\"int\",\"default\":1}]}";

  @Test
  public void testConfigRead() throws IOException {
    String cfg = "{\"minHedgeDelay\" : \"PT1S\", \"maxHedgeDelay\" : \"PT2S\"}";
    TimeoutRelativeHedgePolicy policy = Configs.read(TimeoutRelativeHedgePolicy.class, new StringReader(cfg));
    Assert.assertEquals(Duration.parse("PT1S"), policy.getMinHedgeDelay());
  }

  @Test
  public void testEvolutionConfigRead() throws IOException {
    String cfg = "#Content-Type:" + MediaType.JSON_UTF_8.withParameter("avsc", TEST_SCHEMA) + '\n'
         + "{\"minHedgeDelay\" : \"PT1S\", \"maxHedgeDelay\" : \"PT2S\"}";
    LOG.debug("Test Config", cfg);
    TimeoutRelativeHedgePolicy policy = Configs.read(TimeoutRelativeHedgePolicy.class, new StringReader(cfg));
    Assert.assertEquals(Duration.parse("PT1S"), policy.getMinHedgeDelay());
  }

  @Test
  public void testEvolutionConfigReadYaml() throws IOException {
    String cfg = "#Content-Type:" + MediaType.create("text", "yaml").withParameter("avsc", TEST_SCHEMA) + '\n'
            + "minHedgeDelay: PT1S\nmaxHedgeDelay: PT2S";
    LOG.debug("Test Config", cfg);
    TimeoutRelativeHedgePolicy policy = Configs.read(TimeoutRelativeHedgePolicy.class, new StringReader(cfg));
    Assert.assertEquals(Duration.parse("PT1S"), policy.getMinHedgeDelay());
  }


  @Test
  public void testEvolutionConfigReadYamlFallback() throws IOException {
    String cfg1 = "#Content-Type:" + MediaType.create("text", "yaml").withParameter("avsc", TEST_SCHEMA) + '\n'
            + "minHedgeDelay: PT1S\nmaxHedgeDelay: PT2S";
    LOG.debug("Test Config 1", cfg1);
    String cfg2 = "#Content-Type:" + MediaType.create("text", "yaml").withParameter("avsc", TEST_SCHEMA) + '\n'
            + "maxHedgeDelay: PT10S";
    LOG.debug("Test Config 1", cfg2);

    TimeoutRelativeHedgePolicy policy = Configs.read(TimeoutRelativeHedgePolicy.class,
            SchemaResolver.NONE,
            new StringReader(cfg2), new StringReader(cfg1));
    Assert.assertEquals(Duration.parse("PT1S"), policy.getMinHedgeDelay());
    Assert.assertEquals(Duration.parse("PT10S"), policy.getMaxHedgeDelay());
  }


  @Test
  public void testArbitrary() throws IOException {
    String cfg = "#Content-Type:" + MediaType.JSON_UTF_8.withParameter("avsc", "\"int\"") + '\n'
         +   "123";
    LOG.debug("Test Config", cfg);
    Integer config = Configs.read(Integer.class, new StringReader(cfg));
    Assert.assertEquals((Integer) 123, config);
  }

  @Test
  public void testArbitrary2() throws IOException {
    String cfg = "123";
    LOG.debug("Test Config", cfg);
    Integer config = Configs.read(Integer.class, new StringReader(cfg));
    Assert.assertEquals((Integer) 123, config);
  }

}
