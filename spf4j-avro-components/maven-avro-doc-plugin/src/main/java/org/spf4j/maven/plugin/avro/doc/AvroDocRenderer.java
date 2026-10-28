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

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.avro.JsonProperties;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

/**
 * Renders a set of avro schemas as one self-contained HTML documentation page:
 * a filterable type index grouped by namespace, one section per named type
 * (record, enum or fixed) with doc strings, field tables, defaults and the raw JSON schema.
 * Pure java replacement for the unmaintained node.js based avrodoc tool (https://github.com/ept/avrodoc).
 * The page style (avrodoc.css) and filter script (avrodoc.js) are resources of this plugin,
 * inlined into the generated page.
 *
 * @author Zoltan Farkas
 */
final class AvroDocRenderer {

  private static final String CSS = loadResource("avrodoc.css");

  private static final String JS = loadResource("avrodoc.js");

  private AvroDocRenderer() {
  }

  private static String loadResource(final String name) {
    try (InputStream in = AvroDocRenderer.class.getResourceAsStream(name)) {
      if (in == null) {
        throw new IllegalStateException("Missing resource " + name);
      }
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] chunk = new byte[4096];
      int read;
      while ((read = in.read(chunk)) != -1) {
        buffer.write(chunk, 0, read);
      }
      return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot load resource " + name, ex);
    }
  }

  /**
   * Collect all named types (records, enums, fixed) reachable from the given schemas.
   * First definition of a name wins: the same type is typically inlined in several schema files.
   *
   * @param schema the schema to collect from.
   * @param types the named types collected so far, keyed by full name.
   */
  static void collect(final Schema schema, final Map<String, Schema> types) {
    switch (schema.getType()) {
      case RECORD:
        String fullName = schema.getFullName();
        if (!types.containsKey(fullName)) {
          types.put(fullName, schema);
          for (Schema.Field field : schema.getFields()) {
            collect(field.schema(), types);
          }
        }
        break;
      case ENUM:
      case FIXED:
        String name = schema.getFullName();
        if (!types.containsKey(name)) {
          types.put(name, schema);
        }
        break;
      case ARRAY:
        collect(schema.getElementType(), types);
        break;
      case MAP:
        collect(schema.getValueType(), types);
        break;
      case UNION:
        for (Schema branch : schema.getTypes()) {
          collect(branch, types);
        }
        break;
      default:
        break;
    }
  }

  static int countFields(final Map<String, Schema> types) {
    int count = 0;
    for (Schema schema : types.values()) {
      if (schema.getType() == Schema.Type.RECORD) {
        count += schema.getFields().size();
      }
    }
    return count;
  }

  /**
   * Write the documentation page for the given types to the appendable (a file writer, a string
   * builder, ...), so large schema sets never have to be materialized as one string.
   *
   * @param out the destination.
   * @param title the page title.
   * @param types the named types to document, keyed by full name.
   */
  @SuppressFBWarnings("CBX_CUSTOM_BUILT_XML") // building HTML is the purpose of this method; values are escaped
  static void render(final Appendable out, final String title, final Map<String, Schema> types) throws IOException {
    List<Schema> sorted = new ArrayList<>(types.values());
    Collections.sort(sorted, Comparator.comparing(Schema::getFullName));
    Map<String, List<Schema>> byNamespace = new TreeMap<>();
    for (Schema schema : sorted) {
      String namespace = schema.getNamespace();
      if (namespace == null) {
        namespace = "";
      }
      byNamespace.computeIfAbsent(namespace, (k) -> new ArrayList<>()).add(schema);
    }
    out.append("<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\">");
    out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
    out.append("<title>").append(esc(title)).append(" - avro schema documentation</title>");
    out.append("<style>").append(CSS).append("</style></head><body>\n");
    out.append("<header class=\"top\"><h1>").append(esc(title)).append("</h1>");
    out.append("<p>").append(String.valueOf(sorted.size())).append(" types, ")
            .append(String.valueOf(countFields(types)))
            .append(" fields, ").append(String.valueOf(byNamespace.size())).append(" namespaces</p></header>\n");
    out.append("<div class=\"layout\"><nav class=\"toc\">");
    out.append("<input id=\"filter\" type=\"search\" placeholder=\"Filter types...\" aria-label=\"Filter types\">");
    for (Map.Entry<String, List<Schema>> entry : byNamespace.entrySet()) {
      out.append("<div class=\"ns\">").append(entry.getKey().isEmpty() ? "(no namespace)" : esc(entry.getKey()))
              .append("</div>");
      for (Schema schema : entry.getValue()) {
        out.append("<a href=\"#").append(schema.getFullName()).append("\">")
                .append(esc(schema.getName())).append("</a>");
      }
    }
    out.append("</nav><main>\n");
    for (Schema schema : sorted) {
      renderType(out, schema, types);
    }
    out.append("</main></div>\n");
    out.append("<footer>Generated by the avro-doc goal of the maven-avro-doc-plugin.</footer>\n");
    out.append("<script>").append(JS).append("</script>");
    out.append("</body></html>\n");
  }

  @SuppressFBWarnings("CBX_CUSTOM_BUILT_XML") // building HTML is the purpose of this method; values are escaped
  private static void renderType(final Appendable html, final Schema schema,
          final Map<String, Schema> types) throws IOException {
    Schema.Type type = schema.getType();
    String typeName = type.getName();
    html.append("<section class=\"type\" id=\"").append(schema.getFullName()).append("\">");
    html.append("<h2>").append(esc(schema.getFullName()));
    html.append("<span class=\"badge ").append(typeName).append("\">")
            .append(typeName.toUpperCase(Locale.ROOT)).append("</span></h2>\n");
    Set<String> aliases = schema.getAliases();
    if (!aliases.isEmpty()) {
      html.append("<p class=\"aliases\">Aliases: ");
      boolean first = true;
      for (String alias : aliases) {
        if (!first) {
          html.append(", ");
        }
        first = false;
        html.append("<code>").append(esc(alias)).append("</code>");
      }
      html.append("</p>\n");
    }
    html.append(docHtml(schema.getDoc()));
    switch (type) {
      case RECORD:
        renderRecord(html, schema, types);
        break;
      case ENUM:
        renderEnum(html, schema);
        break;
      case FIXED:
        html.append("<p>Size: <code>").append(String.valueOf(schema.getFixedSize())).append("</code> bytes</p>\n");
        break;
      default:
        break;
    }
    html.append("<details class=\"json\"><summary>JSON schema</summary><pre>")
            .append(esc(schema.toString(true))).append("</pre></details>");
    html.append("</section>\n");
  }

  @SuppressFBWarnings("CBX_CUSTOM_BUILT_XML") // building HTML is the purpose of this method; values are escaped
  private static void renderRecord(final Appendable html, final Schema schema,
          final Map<String, Schema> types) throws IOException {
    List<Schema.Field> fields = schema.getFields();
    if (fields.isEmpty()) {
      html.append("<p>No fields.</p>\n");
      return;
    }
    html.append("<table class=\"fields\"><tr><th>Field</th><th>Type</th><th>Default</th><th>Doc</th></tr>\n");
    for (Schema.Field field : fields) {
      html.append("<tr><td><code>").append(esc(field.name())).append("</code></td><td>")
              .append(typeHtml(field.schema(), types)).append("</td><td class=\"default\"><code>")
              .append(esc(defaultText(field))).append("</code></td><td>")
              .append(docHtmlInline(field.doc())).append("</td></tr>\n");
    }
    html.append("</table>\n");
  }

  @SuppressFBWarnings("CBX_CUSTOM_BUILT_XML") // building HTML is the purpose of this method; values are escaped
  private static void renderEnum(final Appendable html, final Schema schema) throws IOException {
    String enumDefault = schema.getEnumDefault();
    if (enumDefault != null) {
      html.append("<p>Default: <code>").append(esc(enumDefault)).append("</code></p>\n");
    }
    html.append("<p>");
    for (String symbol : schema.getEnumSymbols()) {
      html.append("<span class=\"sym\">").append(esc(symbol)).append("</span>");
    }
    html.append("</p>\n");
  }

  @SuppressFBWarnings("CBX_CUSTOM_BUILT_XML") // building HTML is the purpose of this method; values are escaped
  static String typeHtml(final Schema schema, final Map<String, Schema> types) {
    switch (schema.getType()) {
      case RECORD:
      case ENUM:
      case FIXED:
        String fullName = schema.getFullName();
        if (types.containsKey(fullName)) {
          return "<a href=\"#" + fullName + "\"><code>" + esc(fullName) + "</code></a>";
        } else {
          return "<code>" + esc(fullName) + "</code>";
        }
      case ARRAY:
        return "array&lt;" + typeHtml(schema.getElementType(), types) + "&gt;";
      case MAP:
        return "map&lt;" + typeHtml(schema.getValueType(), types) + "&gt;";
      case UNION:
        StringBuilder union = new StringBuilder();
        for (Schema branch : schema.getTypes()) {
          if (union.length() > 0) {
            union.append(" | ");
          }
          union.append(typeHtml(branch, types));
        }
        return union.toString();
      default:
        String result = esc(schema.getType().getName());
        LogicalType logicalType = schema.getLogicalType();
        if (logicalType != null) {
          result += " <span class=\"logical\">(" + esc(logicalType.getName()) + ")</span>";
        }
        return result;
    }
  }

  static String defaultText(final Schema.Field field) {
    if (!field.hasDefaultValue()) {
      return "";
    }
    Object dflt = field.defaultVal();
    if (dflt == null) {
      return "";
    }
    // Avro represents an explicit JSON null default with the JsonProperties.NULL_VALUE sentinel.
    if (dflt == JsonProperties.NULL_VALUE) {
      return "null";
    }
    if (dflt instanceof JsonNode) {
      return dflt.toString();
    }
    if (dflt instanceof CharSequence) {
      return '"' + dflt.toString() + '"';
    }
    return dflt.toString();
  }

  /**
   * Render an avro doc string as HTML paragraphs. Supports a small markdown subset:
   * paragraphs, `code`, **bold** and [text](http...) links.
   */
  static String docHtml(final String doc) {
    if (doc == null || doc.trim().isEmpty()) {
      return "";
    }
    StringBuilder result = new StringBuilder("<div class=\"doc\">");
    for (String paragraph : doc.trim().split("\n\\s*\n")) {
      result.append("<p>").append(inline(paragraph.replace('\n', ' '))).append("</p>");
    }
    return result.append("</div>\n").toString();
  }

  private static String docHtmlInline(final String doc) {
    if (doc == null || doc.trim().isEmpty()) {
      return "";
    }
    return inline(doc.replace('\n', ' '));
  }

  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS") // the indexOf calls scan different offsets
  private static String inline(final String text) {
    // Code spans are literal: no link/bold markup is applied inside backticks.
    // An unmatched backtick is treated as plain text.
    StringBuilder result = new StringBuilder(text.length());
    int pos = 0;
    while (pos < text.length()) {
      int open = text.indexOf('`', pos);
      int close = open < 0 ? -1 : text.indexOf('`', open + 1);
      if (close < 0) {
        result.append(markup(esc(text.substring(pos))));
        break;
      }
      result.append(markup(esc(text.substring(pos, open))));
      result.append("<code>").append(esc(text.substring(open + 1, close))).append("</code>");
      pos = close + 1;
    }
    return result.toString();
  }

  private static String markup(final String escapedText) {
    return escapedText
            .replaceAll("\\[([^\\]]+)\\]\\((https?://[^)\\s]+)\\)", "<a href=\"$2\">$1</a>")
            .replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
  }

  static String esc(final String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
  }

}
