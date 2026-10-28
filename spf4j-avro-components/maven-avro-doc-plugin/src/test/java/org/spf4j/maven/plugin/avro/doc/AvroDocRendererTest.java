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
package org.spf4j.maven.plugin.avro.doc;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Zoltan Farkas
 */
public class AvroDocRendererTest {

  private static final String SCHEMA = "{"
          + "\"type\":\"record\",\"name\":\"TestRecord\",\"namespace\":\"org.example\","
          + "\"doc\":\"A **test** record with `code`.\","
          + "\"fields\":["
          + "{\"name\":\"status\",\"doc\":\"the status\","
          + "\"type\":{\"type\":\"enum\",\"name\":\"Status\",\"symbols\":[\"NEW\",\"DONE\"],"
          + "\"default\":\"NEW\",\"doc\":\"status enum\"},\"default\":\"NEW\"},"
          + "{\"name\":\"hash\",\"type\":{\"type\":\"fixed\",\"name\":\"Hash\",\"size\":16}},"
          + "{\"name\":\"count\",\"type\":[\"null\",\"long\"],\"default\":null,\"doc\":\"how many\"},"
          + "{\"name\":\"name\",\"type\":\"string\",\"default\":\"abc\"}"
          + "]}";

  @Test
  public void testCollectAndRender() {
    Schema schema = new Schema.Parser().parse(SCHEMA);
    Map<String, Schema> types = new LinkedHashMap<>();
    AvroDocRenderer.collect(schema, types);
    Assert.assertEquals(3, types.size());
    Assert.assertTrue(types.containsKey("org.example.TestRecord"));
    Assert.assertTrue(types.containsKey("org.example.Status"));
    Assert.assertTrue(types.containsKey("org.example.Hash"));
    Assert.assertEquals(4, AvroDocRenderer.countFields(types));
    String html = AvroDocRenderer.render("test title", types);
    Assert.assertTrue(html.contains("id=\"org.example.TestRecord\""));
    Assert.assertTrue(html.contains("href=\"#org.example.Status\""));
    Assert.assertTrue(html.contains("<span class=\"sym\">DONE</span>"));
    Assert.assertTrue(html.contains("Size: <code>16</code> bytes"));
    Assert.assertTrue(html.contains("&quot;abc&quot;"));
    Assert.assertTrue(html.contains("<strong>test</strong>"));
    Assert.assertTrue(html.contains("<code>code</code>"));
    Assert.assertTrue(html.contains("null"));
  }

  @Test
  public void testEscaping() {
    Assert.assertEquals("a&lt;b&gt;&amp;&quot;c&quot;", AvroDocRenderer.esc("a<b>&\"c\""));
  }

  @Test
  public void testParseUndefinedLogicalType() throws Exception {
    String json = "{\"type\":\"record\",\"name\":\"Any\",\"namespace\":\"org.example\","
            + "\"logicalType\":\"any\",\"doc\":\"Any type\","
            + "\"fields\":[{\"name\":\"content\",\"type\":\"bytes\"}]}";
    java.io.File file = java.io.File.createTempFile("any", ".avsc");
    java.nio.file.Files.write(file.toPath(), json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    Schema schema = SchemaDocMojo.parseSchema(file);
    Map<String, Schema> types = new LinkedHashMap<>();
    AvroDocRenderer.collect(schema, types);
    Assert.assertTrue(types.containsKey("org.example.Any"));
    Assert.assertTrue(AvroDocRenderer.render("t", types).contains("org.example.Any"));
    java.nio.file.Files.delete(file.toPath());
  }

}
