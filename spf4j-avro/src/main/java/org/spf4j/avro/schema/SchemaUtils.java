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
package org.spf4j.avro.schema;

import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.JsonProperties;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.spf4j.ds.Graphs;
import org.spf4j.io.AppendableWriter;

/**
 * @author zoly
 */
public final class SchemaUtils {

  private static final boolean HAS_IDL_CYCLE_DEF_SUPPORT;

  static {
    boolean hasIDLCycleSupp = true;
    try {
      Class.forName("org.apache.avro.compiler.idl.ResolvingVisitor");
    } catch (ClassNotFoundException ex) {
      hasIDLCycleSupp = false;
    }
    HAS_IDL_CYCLE_DEF_SUPPORT = hasIDLCycleSupp;
  }


  private static final JsonFactory JSON_FACT = new JsonFactory();

  public static final BiConsumer<Schema, Schema> SCHEMA_ESENTIALS
          = (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyLogicalTypes(a, b);
          };

  public static final BiConsumer<Schema.Field, Schema.Field> FIELD_ESENTIALS
          = (a, b) -> {
            SchemaUtils.copyAliases(a, b);
          };

  public static final BiConsumer<Schema, Schema> SCHEMA_EVERYTHING
          = (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyLogicalTypes(a, b);
            SchemaUtils.copyProperties(a, b);
          };

  public static final BiConsumer<Schema.Field, Schema.Field> FIELD_EVERYTHING
          = (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyProperties(a, b);
          };

  private SchemaUtils() {
  }

  public static boolean isIdlCycleSupport() {
    return HAS_IDL_CYCLE_DEF_SUPPORT;
  }



  public static void copyAliases(final Schema from, final Schema to) {
    //CHECKSTYLE:OFF
    switch (from.getType()) { // only named types.
      case RECORD:
      case ENUM:
      case FIXED:
        Set<String> aliases = from.getAliases();
        for (String alias : aliases) {
          to.addAlias(alias);
        }
        break;
      default:
      //ignore unnamed one's
    }
    //CHECKSTYLE:OFF
  }

  public static void copyAliases(final Schema.Field from, final Schema.Field to) {
    Set<String> aliases = from.aliases();
    for (String alias : aliases) {
      to.addAlias(alias);
    }
  }

  public static void copyLogicalTypes(final Schema from, final Schema to) {
    LogicalType logicalType = from.getLogicalType();
    if (logicalType != null) {
      logicalType.addToSchema(to);
    }
  }

  public static void copyProperties(final JsonProperties from, final JsonProperties to) {
    Map<String, Object> objectProps = from.getObjectProps();
    for (Map.Entry<String, Object> entry : objectProps.entrySet()) {
      to.addProp(entry.getKey(), entry.getValue());
    }
  }

  public static boolean hasGeneratedJavaClass(final Schema schema) {
    Schema.Type type = schema.getType();
    switch (type) {
      case ENUM:
      case RECORD:
      case FIXED:
        return true;
      default:
        return false;
    }
  }

  public static String getJavaClassName(final Schema schema) {
    String namespace = schema.getNamespace();
    if (namespace == null || namespace.isEmpty()) {
      return SpecificCompiler.mangle(schema.getName());
    } else {
      return namespace + '.' + SpecificCompiler.mangle(schema.getName());
    }
  }

  @Beta
  public static void writeIdlProtocol(final String protocolName, final String protocolNameSpace,
          final Appendable appendable, final Schema... schemas) throws IOException {
    if (protocolNameSpace != null) {
      appendable.append("@namespace(\"").append(protocolNameSpace).append("\")\n");
    }
    appendable.append("protocol ").append(protocolName).append(" {\n\n");
    if (isIdlCycleSupport()) {
      writeIdl(appendable, new HashSet<String>(4), protocolNameSpace, schemas);
    } else {
      writeIdlLegacy(appendable, new HashSet<String>(4), protocolNameSpace, schemas);
    }
    appendable.append("}\n");
  }

  @Beta
  public static void writeIdl(final Appendable appendable,
          final Set<String> alreadyDeclared, final String protocolNameSpace, final Schema ... pschemas)
          throws IOException {
    try(JsonGenerator jsonGen = createJsonGenerator(appendable)) {
      final Set<Schema> toDeclare = new HashSet<>(4);
      toDeclare.addAll(Arrays.asList(pschemas));
      while (!toDeclare.isEmpty()) {
        Iterator<Schema> iterator = toDeclare.iterator();
        Schema schema = iterator.next();
        iterator.remove();
        writeSchema(schema, appendable, jsonGen, protocolNameSpace, alreadyDeclared, toDeclare);
        appendable.append('\n');
      }
    }
  }

  public static JsonGenerator createJsonGenerator(final Appendable appendable) throws IOException {
    JsonGenerator jsonGen;
    if (appendable instanceof Writer) {
      jsonGen = JSON_FACT.createJsonGenerator((Writer) appendable);
    } else {
      jsonGen = JSON_FACT.createJsonGenerator(new AppendableWriter(appendable));
    }
    return jsonGen;
  }

  @Beta
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public static void writeIdlLegacy(final Appendable appendable,
          final Set<String> alreadyDeclared, final String protocolNameSpace, final Schema ... pschemas)
          throws IOException {
    MutableGraph<Schema> schemaDeps = GraphBuilder.directed().allowsSelfLoops(false)
            .expectedNodeCount(4).build();
    Map<Schema, String> idlRepresentation = new HashMap<>(4);
    final Set<Schema> toDeclare = new HashSet<>(4);
    toDeclare.addAll(Arrays.asList(pschemas));
    while (!toDeclare.isEmpty()) {
      Iterator<Schema> iterator = toDeclare.iterator();
      Schema schema = iterator.next();
      iterator.remove();
      StringWriter schemaIdrStr = new StringWriter();
      JsonGenerator jsonGen = JSON_FACT.createJsonGenerator(schemaIdrStr);
      Set<Schema> orig = new HashSet<>(toDeclare);
      writeSchema(schema, schemaIdrStr, jsonGen, protocolNameSpace, alreadyDeclared, toDeclare);
      idlRepresentation.put(schema, schemaIdrStr.toString());
      schemaDeps.addNode(schema);
      Set<Schema> dependencies = Sets.difference(toDeclare, orig);
      for (Schema dep : dependencies) {
        schemaDeps.putEdge(schema, dep);
      }
     }
     // Now process the nodes from leaves onward.

    MutableGraph<Schema> traverseGraph = Graphs.clone(schemaDeps);
    Set<Schema> nodes = traverseGraph.nodes();
    List<Schema> nodesToRemove = new ArrayList<>();
    do {
      for (Schema token : nodes) {
        if (traverseGraph.outDegree(token) == 0) {
          nodesToRemove.add(token);
          appendable.append(idlRepresentation.get(token));
          appendable.append('\n');
        }
      }
      if (nodesToRemove.isEmpty() && !nodes.isEmpty()) {
        throw new IllegalArgumentException("Schema definition cycle for" + nodes);
      }
      for (Schema token : nodesToRemove) {
        traverseGraph.removeNode(token);
      }
      nodesToRemove.clear();
      nodes = traverseGraph.nodes();
    } while (!nodes.isEmpty());


  }


  private static void writeSchema(Schema schema, final Appendable appendable, JsonGenerator jsonGen,
          final String protocolNameSpace, final Set<String> alreadyDeclared, final Set<Schema> toDeclare)
          throws IOException {
    Schema.Type type = schema.getType();
    writeSchemaAttributes(schema, appendable, jsonGen, true);
    String namespace = schema.getNamespace();
    if (!Objects.equals(namespace, protocolNameSpace)) {
      appendable.append("@namespace(\"").append(namespace).append("\")\n");
    }
    Set<String> saliases = schema.getAliases();
    if (!saliases.isEmpty()) {
      appendable.append("@aliases(");
      toJson(saliases, jsonGen);
      jsonGen.flush();
      appendable.append(")\n");
    }
    switch (type) {
      case RECORD:
        appendable.append("record ").append(schema.getName()).append(" {\n\n");
        alreadyDeclared.add(schema.getFullName());
        for (Field field : schema.getFields()) {
          String fDoc = field.doc();
          if (fDoc != null) {
            appendable.append("  /** ").append(fDoc).append(" */\n");
          }
          appendable.append("  ");
          writeFieldSchema(field.schema(), appendable, jsonGen, alreadyDeclared, toDeclare, schema.getNamespace());
          appendable.append(' ');
          Set<String> faliases = field.aliases();
          if (!faliases.isEmpty()) {
            appendable.append("@aliases(");
            toJson(faliases, jsonGen);
            jsonGen.flush();
            appendable.append(") ");
          }
          Field.Order order = field.order();
          if (order != null) {
            appendable.append(" @order(\"").append(order.name()).append("\") ");
          }
          writeJsonProperties(field, appendable, jsonGen, false);
          appendable.append(' ');
          appendable.append(field.name());
          JsonNode defaultValue = field.defaultValue();
          if (defaultValue != null) {
            appendable.append(" = ");
            toJson(field.defaultVal(), jsonGen);
            jsonGen.flush();
          }
          appendable.append(";\n\n");
        }
        appendable.append("}\n");
        break;
      case ARRAY:
      case MAP:
      case UNION:
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case NULL:
      case STRING:
        throw new UnsupportedOperationException("No IDL entity, nust be part of a record: " + schema);
      case ENUM:
        appendable.append("enum ").append(schema.getName()).append(" {");
        alreadyDeclared.add(schema.getFullName());
        Iterator<String> i = schema.getEnumSymbols().iterator();
        if (i.hasNext()) {
          appendable.append(i.next());
          while (i.hasNext()) {
            appendable.append(',');
            appendable.append(i.next());
          }
        } else {
          throw new IllegalStateException("Enum schema must have at least a symbol " + schema);
        }
        appendable.append("}\n");
        break;
      case FIXED:
        appendable.append("fixed ").append(schema.getName()).append('(')
                .append(Integer.toString(schema.getFixedSize())).append(");\n");
        alreadyDeclared.add(schema.getFullName());
        break;
      default:
        throw new IllegalStateException("Unsupported schema " + schema);
    }
  }

  private static void writeFieldSchema(final Schema schema,
          final Appendable appendable, final JsonGenerator jsonGen,
          final Set<String> alreadyDeclared, final Set<Schema> toDeclare,
          final String recordNameSpace) throws IOException {
    Schema.Type type = schema.getType();
    switch (type) {
      case RECORD:
      case ENUM:
      case FIXED:
        if (Objects.equals(recordNameSpace, schema.getNamespace())) {
          appendable.append(schema.getName());
        } else {
          appendable.append(schema.getFullName());
        }
        if (!alreadyDeclared.contains(schema.getFullName())) {
          toDeclare.add(schema);
        }
        break;
      case ARRAY:
        writeSchemaAttributes(schema, appendable, jsonGen, false);
        appendable.append("array<");
        writeFieldSchema(schema.getElementType(), appendable, jsonGen, alreadyDeclared, toDeclare, recordNameSpace);
        appendable.append('>');
        break;
      case MAP:
        writeSchemaAttributes(schema, appendable, jsonGen, false);
        appendable.append("map<");
        writeFieldSchema(schema.getValueType(), appendable, jsonGen, alreadyDeclared, toDeclare, recordNameSpace);
        appendable.append('>');
        break;
      case UNION:
        writeSchemaAttributes(schema, appendable, jsonGen, false);
        appendable.append("union {");
        List<Schema> types = schema.getTypes();
        Iterator<Schema> iterator = types.iterator();
        if (iterator.hasNext()) {
          writeFieldSchema(iterator.next(), appendable, jsonGen, alreadyDeclared, toDeclare, recordNameSpace);
          while (iterator.hasNext()) {
            appendable.append(',');
            writeFieldSchema(iterator.next(), appendable, jsonGen, alreadyDeclared, toDeclare, recordNameSpace);
          }
        } else {
          throw new IllegalStateException("Union schmemas must have member types " + schema);
        }
        appendable.append('}');
        break;
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case NULL:
      case STRING:
        writeSchemaAttributes(schema, appendable, jsonGen, false);
        appendable.append(schema.getName());
        break;
      default:
        throw new IllegalStateException("Unsupported schema " + schema);
    }
  }

  public static void writeSchemaAttributes(final Schema schema,
          final Appendable appendable, final JsonGenerator jsonGen, final boolean crBetween)
          throws IOException {
    String doc = schema.getDoc();
    if (doc != null) {
      appendable.append("/** ").append(doc).append(" */");
      if (crBetween) {
        appendable.append('\n');
      } else {
        appendable.append(' ');
      }
    }
    writeJsonProperties(schema, appendable, jsonGen, crBetween);
  }

  public static void writeJsonProperties(final JsonProperties props,
          final Appendable appendable, final JsonGenerator jsonGen, final boolean crBetween) throws IOException {
    Map<String, Object> objectProps = props.getObjectProps();
    for (Map.Entry<String, Object> entry : objectProps.entrySet()) {
      appendable.append('@').append(entry.getKey()).append('(');
      toJson(entry.getValue(), jsonGen);
      jsonGen.flush();
      appendable.append(')');
      if (crBetween) {
        appendable.append('\n');
      } else {
        appendable.append(' ');
      }
    }
  }

  @SuppressWarnings(value="unchecked")
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  static void toJson(final Object datum, final JsonGenerator generator) throws IOException {
    if (datum == JsonProperties.NULL_VALUE) { // null
      generator.writeNull();
    } else if (datum instanceof Map) { // record, map
      generator.writeStartObject();
      for (Map.Entry<Object,Object> entry : ((Map<Object,Object>) datum).entrySet()) {
        generator.writeFieldName(entry.getKey().toString());
        toJson(entry.getValue(), generator);
      }
      generator.writeEndObject();
    } else if (datum instanceof Collection) { // array
      generator.writeStartArray();
      for (Object element : (Collection<?>) datum) {
        toJson(element, generator);
      }
      generator.writeEndArray();
    } else if (datum instanceof byte[]) { // bytes, fixed
      generator.writeString(new String((byte[]) datum, StandardCharsets.ISO_8859_1));
    } else if (datum instanceof CharSequence || datum instanceof Enum<?>) { // string, enum
      generator.writeString(datum.toString());
    } else if (datum instanceof Double) { // double
      generator.writeNumber((Double) datum);
    } else if (datum instanceof Float) { // float
      generator.writeNumber((Float) datum);
    } else if (datum instanceof Long) { // long
      generator.writeNumber((Long) datum);
    } else if (datum instanceof Integer) { // int
      generator.writeNumber((Integer) datum);
    } else if (datum instanceof Boolean) { // boolean
      generator.writeBoolean((Boolean) datum);
    } else {
      throw new AvroRuntimeException("Unknown datum class: " + datum.getClass());
    }
  }

}
