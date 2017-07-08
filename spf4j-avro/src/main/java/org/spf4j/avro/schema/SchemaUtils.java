package org.spf4j.avro.schema;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apache.avro.JsonProperties;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;

/**
 * @author zoly
 */
public final class SchemaUtils {

  public static final BiConsumer<Schema, Schema> SCHEMA_ESENTIALS =
          (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyLogicalTypes(a, b);
          };


  public static final BiConsumer<Schema.Field, Schema.Field> FIELD_ESENTIALS =
          (a, b) -> {
            SchemaUtils.copyAliases(a, b);
          };


  public static final BiConsumer<Schema, Schema> SCHEMA_EVERYTHING =
          (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyLogicalTypes(a, b);
            SchemaUtils.copyProperties(a, b);
          };

  public static final BiConsumer<Schema.Field, Schema.Field> FIELD_EVERYTHING =
          (a, b) -> {
            SchemaUtils.copyAliases(a, b);
            SchemaUtils.copyProperties(a, b);
          };



  private SchemaUtils() {
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

}
