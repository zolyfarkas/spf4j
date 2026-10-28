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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

/**
 * Renders a set of avro schemas as one self-contained HTML documentation page:
 * a filterable type index grouped by namespace, one section per named type
 * (record, enum or fixed) with doc strings, field tables, defaults and the raw JSON schema.
 * Pure java replacement for the unmaintained node.js based avrodoc tool (https://github.com/ept/avrodoc).
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("CBX_CUSTOM_BUILT_XML") // building HTML is the purpose of this class; all values are escaped
final class AvroDocRenderer {

  private static final String CSS = "body{margin:0;font-family:-apple-system,\"Segoe UI\",Roboto,"
          + "\"Helvetica Neue\",Arial,sans-serif;color:#1f2933;background:#f5f7fa}"
          + "header.top{background:#102a43;color:#fff;padding:1.2em 2em}"
          + "header.top h1{margin:0;font-size:1.5em}"
          + "header.top p{margin:.3em 0 0;color:#bcccdc}"
          + ".layout{display:flex;align-items:flex-start}"
          + "nav.toc{position:sticky;top:0;width:300px;min-width:300px;height:100vh;overflow:auto;"
          + "background:#fff;border-right:1px solid #d9e2ec;padding:1em}"
          + "nav.toc input{width:100%;padding:.45em .6em;border:1px solid #d9e2ec;border-radius:4px;"
          + "margin-bottom:.8em}"
          + "nav.toc .ns{font-weight:600;color:#627d98;margin:.8em 0 .3em;font-size:.85em;"
          + "text-transform:uppercase;letter-spacing:.04em}"
          + "nav.toc a{display:block;padding:.18em .4em;color:#0b5cad;text-decoration:none;border-radius:3px;"
          + "font-family:ui-monospace,Menlo,Consolas,monospace;font-size:.85em}"
          + "nav.toc a:hover{background:#f5f7fa}"
          + "main{flex:1;padding:1.5em 2em;max-width:1100px}"
          + "section.type{background:#fff;border:1px solid #d9e2ec;border-radius:6px;margin-bottom:1.5em;"
          + "padding:1em 1.2em}"
          + "section.type h2{margin:.1em 0 .4em;font-family:ui-monospace,Menlo,Consolas,monospace;font-size:1.15em}"
          + ".badge{display:inline-block;vertical-align:middle;font-size:.65em;font-weight:700;"
          + "letter-spacing:.05em;padding:.15em .5em;border-radius:3px;color:#fff;margin-left:.5em}"
          + ".badge.record{background:#0b5cad}.badge.enum{background:#7c3aed}.badge.fixed{background:#627d98}"
          + ".aliases{color:#627d98;font-size:.9em}"
          + "table.fields{border-collapse:collapse;width:100%;margin-top:.6em}"
          + "table.fields th,table.fields td{border-top:1px solid #d9e2ec;padding:.45em .6em;text-align:left;"
          + "vertical-align:top}"
          + "table.fields th{color:#627d98;font-size:.8em;text-transform:uppercase;letter-spacing:.04em}"
          + "code{font-family:ui-monospace,Menlo,Consolas,monospace;background:#f0f4f8;padding:.05em .3em;"
          + "border-radius:3px}"
          + "td.default code{background:none;color:#627d98}"
          + ".sym{display:inline-block;background:#f3e8ff;border:1px solid #e9d5ff;color:#5b21b6;"
          + "border-radius:3px;padding:.1em .5em;margin:.15em .2em .15em 0;"
          + "font-family:ui-monospace,Menlo,Consolas,monospace;font-size:.85em}"
          + "details.json{margin-top:.8em}details.json summary{cursor:pointer;color:#0b5cad;font-size:.9em}"
          + "details.json pre{background:#102a43;color:#d9e2ec;padding:1em;border-radius:6px;overflow:auto;"
          + "font-size:.8em}"
          + "a{color:#0b5cad}.logical{color:#627d98;font-size:.85em}.doc p{margin:.4em 0}"
          + "footer{color:#627d98;padding:0 2em 2em;font-size:.85em}"
          + "@media(max-width:800px){.layout{flex-direction:column}nav.toc{position:static;width:100%;"
          + "min-width:0;height:auto}}";

  private static final String JS = "document.getElementById('filter').addEventListener('input',function(e){"
          + "var q=e.target.value.toLowerCase();"
          + "document.querySelectorAll('section.type').forEach(function(s){"
          + "s.style.display=s.id.toLowerCase().indexOf(q)>=0?'':'none';});"
          + "document.querySelectorAll('nav.toc a').forEach(function(a){"
          + "a.style.display=a.textContent.toLowerCase().indexOf(q)>=0?'':'none';});});";

  private AvroDocRenderer() {
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

  static String render(final String title, final Map<String, Schema> types) {
    List<Schema> sorted = new ArrayList<>(types.values());
    Collections.sort(sorted, Comparator.comparing(Schema::getFullName));
    Map<String, List<Schema>> byNamespace = new TreeMap<>();
    for (Schema schema : sorted) {
      String namespace = schema.getNamespace();
      if (namespace == null) {
        namespace = "";
      }
      List<Schema> group = byNamespace.get(namespace);
      if (group == null) {
        group = new ArrayList<>();
        byNamespace.put(namespace, group);
      }
      group.add(schema);
    }
    StringBuilder html = new StringBuilder(65536);
    html.append("<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\">");
    html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
    html.append("<title>").append(esc(title)).append(" - avro schema documentation</title>");
    html.append("<style>").append(CSS).append("</style></head><body>\n");
    html.append("<header class=\"top\"><h1>").append(esc(title)).append("</h1>");
    html.append("<p>").append(sorted.size()).append(" types, ").append(countFields(types))
            .append(" fields, ").append(byNamespace.size()).append(" namespaces</p></header>\n");
    html.append("<div class=\"layout\"><nav class=\"toc\">");
    html.append("<input id=\"filter\" type=\"search\" placeholder=\"Filter types...\" aria-label=\"Filter types\">");
    for (Map.Entry<String, List<Schema>> entry : byNamespace.entrySet()) {
      html.append("<div class=\"ns\">").append(entry.getKey().isEmpty() ? "(no namespace)" : esc(entry.getKey()))
              .append("</div>");
      for (Schema schema : entry.getValue()) {
        html.append("<a href=\"#").append(schema.getFullName()).append("\">")
                .append(esc(schema.getName())).append("</a>");
      }
    }
    html.append("</nav><main>\n");
    for (Schema schema : sorted) {
      renderType(html, schema, types);
    }
    html.append("</main></div>\n");
    html.append("<footer>Generated by the avro-doc goal of the maven-avro-schema-plugin.</footer>\n");
    html.append("<script>").append(JS).append("</script>");
    html.append("</body></html>\n");
    return html.toString();
  }

  private static void renderType(final StringBuilder html, final Schema schema, final Map<String, Schema> types) {
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
        html.append("<p>Size: <code>").append(schema.getFixedSize()).append("</code> bytes</p>\n");
        break;
      default:
        break;
    }
    html.append("<details class=\"json\"><summary>JSON schema</summary><pre>")
            .append(esc(schema.toString(true))).append("</pre></details>");
    html.append("</section>\n");
  }

  private static void renderRecord(final StringBuilder html, final Schema schema, final Map<String, Schema> types) {
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

  private static void renderEnum(final StringBuilder html, final Schema schema) {
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
    // Avro represents an explicit JSON null default with a JsonProperties$Null instance,
    // whose toString is a useless class@hash; render it as null.
    if (dflt.getClass().getName().endsWith("$Null")) {
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

  private static String inline(final String text) {
    return esc(text)
            .replaceAll("`([^`]+)`", "<code>$1</code>")
            .replaceAll("\\[([^\\]]+)\\]\\((https?://[^)\\s]+)\\)", "<a href=\"$2\">$1</a>")
            .replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
  }

  static String esc(final String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

}
