package org.apache.avro.reflect;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.apache.avro.AvroTypeException;
import org.apache.avro.JsonProperties;
import org.apache.avro.Schema;
import org.apache.avro.data.Json;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.specific.FixedSize;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecordBase;

/**
 * Duplicate for zolyfarkas/avro fork.
 *
 * @author Zoltan Farkas
 */
public final class ExtendedReflectData extends ReflectData {

  private static final ExtendedReflectData INSTANCE = new ExtendedReflectData();

  private static final Schema NONE = Schema.create(Schema.Type.NULL);

  private final ConcurrentMap<Type, Schema> type2SchemaMap = new ConcurrentHashMap<>();

  public static ExtendedReflectData get() {
    return INSTANCE;
  }

  @Override
  @Nullable
  public Schema getSchema(final Type type) {
    Schema result = type2SchemaMap.computeIfAbsent(type, (t) -> {
      try {
        return createSchema(t, new HashMap<>());
      } catch (RuntimeException ex) {
        Logger.getLogger(ExtendedReflectData.class.getName())
                .log(Level.FINE, "Unable to create schema for " + type, ex);
        return NONE;
      }
    });
    return result == NONE ? null : result;
  }

  @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
  public Schema createSchema(final Type type, final Object object, final Map<String, Schema> names) {
    if (type instanceof TypeVariable) {
      String name = ((TypeVariable) type).getName();
      Schema schema = names.get(name);
      if (schema == null) {
        throw new IllegalArgumentException("Undefined type variable " + type);
      }
      return schema;
    }

    if (type instanceof GenericArrayType) {                  // generic array
      Type component = ((GenericArrayType) type).getGenericComponentType();
      if (component == Byte.TYPE) { // byte array
        return Schema.create(Schema.Type.BYTES);
      }
      Schema result;
      if (java.lang.reflect.Array.getLength(object) > 0) {
        result = Schema.createArray(createSchema(component, java.lang.reflect.Array.get(object, 0), names));
        setElement(result, component);
      } else {
        result = Schema.createArray(Schema.create(Schema.Type.NULL));
        setElement(result, component);
      }
      return result;
    } else if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType) type;
      Class raw = (Class) ptype.getRawType();
      Type[] params = ptype.getActualTypeArguments();
      if (Map.class.isAssignableFrom(raw)) {  // Map
        Map map = (Map) object;
        Class key = (Class) params[0];
        if (isStringable(key)) {                             // Stringable key
          Schema schema;
          if (map.isEmpty()) {
            schema = Schema.createMap(Schema.create(Schema.Type.NULL));
          } else {
            schema = Schema.createMap(createSchema(params[1], map.values().iterator().next(), names));
          }
          schema.addProp(SpecificData.CLASS_PROP, key.getName());
          return schema;
        } else if (key != String.class) {
          Schema schema;
          if (map.isEmpty()) {
            schema = Schema.createMap(Schema.create(Schema.Type.NULL));
          } else {
            Map.Entry next = (Map.Entry) map.entrySet().iterator().next();
            schema = createNonStringMapSchema(next.getKey().getClass(), next.getKey().getClass(), names);
          }
          schema.addProp(SpecificData.CLASS_PROP, raw.getName());
          return schema;
        }
      } else if (Collection.class.isAssignableFrom(raw)) {   // Collection
        Iterable col = (Iterable) object;
        Iterator iterator = col.iterator();
        if (iterator.hasNext()) {
          Schema schema = Schema.createArray(createSchema(params[0], iterator.next(), names));
          schema.addProp(SpecificData.CLASS_PROP, raw.getName());
          return schema;
        } else {
          Schema schema = Schema.createArray(Schema.create(Schema.Type.NULL));
          schema.addProp(SpecificData.CLASS_PROP, raw.getName());
          return schema;
        }
      }
    } else if ((type == Byte.class) || (type == Byte.TYPE)) {
      Schema result = Schema.create(Schema.Type.INT);
      result.addProp(SpecificData.CLASS_PROP, Byte.class.getName());
      return result;
    } else if ((type == Short.class) || (type == Short.TYPE)) {
      Schema result = Schema.create(Schema.Type.INT);
      result.addProp(SpecificData.CLASS_PROP, Short.class.getName());
      return result;
    } else if ((type == Character.class) || (type == Character.TYPE)) {
      Schema result = Schema.create(Schema.Type.INT);
      result.addProp(SpecificData.CLASS_PROP, Character.class.getName());
      return result;
    } else if (type instanceof Class) {
      return createSchemaFromClass((Class<?>) type, object, names);
    }
    return super.createSchema(type, names);
  }

  private Schema createSchemaFromClass(final Class<?> c, final Object object, final Map<String, Schema> names) {
    if (c == Void.class || c == Boolean.class
            || // primitives
            c == Integer.class || c == Long.class
            || c == Float.class || c == Double.class
            || c == Byte.class || c == Short.class
            || c == Character.class || c.isPrimitive()) {
      return super.createSchema(c, names);
    }
    if (c.isArray()) {                                     // array
      Class component = c.getComponentType();
      if (component == Byte.TYPE) {                        // byte array
        Schema result = Schema.create(Schema.Type.BYTES);
        result.addProp(SpecificData.CLASS_PROP, c.getName());
        return result;
      }
      Schema result = Schema.createArray(createSchema(component, names));
      result.addProp(SpecificData.CLASS_PROP, c.getName());
      setElement(result, component);
      return result;
    }
    AvroSchema explicit = c.getAnnotation(AvroSchema.class);
    if (explicit != null) {                                 // explicit schema
      return new Schema.Parser().parse(explicit.value());
    }
    if (CharSequence.class.isAssignableFrom(c)) {           // String
      return Schema.create(Schema.Type.STRING);
    }
    if (ByteBuffer.class.isAssignableFrom(c)) {             // bytes
      return Schema.create(Schema.Type.BYTES);
    }
    if (Iterable.class.isAssignableFrom(c)) {
      Iterable col = (Iterable) object;
      Iterator iterator = col.iterator();
      if (iterator.hasNext()) {
        Object next = iterator.next();
        return Schema.createArray(createSchema(next.getClass(), next, names));
      } else {
        return Schema.createArray(Schema.create(Schema.Type.NULL));
      }
    }
    if (Map.class.isAssignableFrom(c)) {
      Map map = (Map) object;
      if (map.isEmpty()) {
        return Schema.createMap(Schema.create(Schema.Type.NULL));
      }
      Map.Entry elem = (Map.Entry) map.entrySet().iterator().next();
      Class key = elem.getKey().getClass();
      if (isStringable(key) || CharSequence.class.isAssignableFrom(key)) {   // Stringable key
        Schema schema = Schema.createMap(createSchema(elem.getValue().getClass(), elem.getValue(), names));
        if (key != String.class) {
          schema.addProp(SpecificData.CLASS_PROP, key.getName());
        }
        return schema;
      } else if (key != String.class) {
        return createNonStringMapSchema(elem.getKey().getClass(), elem.getValue().getClass(), names);
      }
    }
    String fullName = c.getName();
    Schema schema = names.get(fullName);
    if (schema == null) {
      AvroDoc annotatedDoc = c.getAnnotation(AvroDoc.class);    // Docstring
      String doc = (annotatedDoc != null) ? annotatedDoc.value() : null;
      String name = c.getSimpleName();
      String space = c.getPackage() == null ? "" : c.getPackage().getName();
      Class<?> enclosingClass = c.getEnclosingClass();
      if (enclosingClass != null) {                 // nested class
        space = enclosingClass.getName();
      }
      Union union = c.getAnnotation(Union.class);
      if (union != null) {                                 // union annotated
        return getAnnotatedUnion(union, names);
      } else if (isStringable(c)) {                        // Stringable
        Schema result = Schema.create(Schema.Type.STRING);
        result.addProp(SpecificData.CLASS_PROP, c.getName());
        return result;
      } else if (c.isEnum()) {                             // Enum
        Enum[] constants = (Enum[]) c.getEnumConstants();
        List<String> symbols = new ArrayList<>(constants.length);
        for (int i = 0; i < constants.length; i++) {
          symbols.add(constants[i].name());
        }
        schema = Schema.createEnum(name, doc, space, symbols);
        consumeAvroAliasAnnotation(c, schema);
      } else if (GenericFixed.class.isAssignableFrom(c)) { // fixed
        int size = c.getAnnotation(FixedSize.class).value();
        schema = Schema.createFixed(name, doc, space, size);
        consumeAvroAliasAnnotation(c, schema);
      } else if (IndexedRecord.class.isAssignableFrom(c)) { // specific
        if (SpecificRecordBase.class.isAssignableFrom(c)) {
          return super.createSchema(c, names);
        } else {
          return ((GenericRecord) object).getSchema();
        }
      } else {
        schema = createRecordSchema(c, name, space, doc, names);
      }
      names.put(fullName, schema);
    }
    return schema;
  }

  private Schema createRecordSchema(final Class<?> c, final String name,
           final String space, final String doc, final Map<String, Schema> names) {
    // record
    boolean error = Throwable.class.isAssignableFrom(c);
    Schema schema = Schema.createRecord(name, doc, space, error);
    consumeAvroAliasAnnotation(c, schema);
    names.put(c.getName(), schema);
    Field[] cachedFields = getCachedFields(c);
    List<Schema.Field> fields = new ArrayList<>(cachedFields.length);
    for (Field field : cachedFields) {
      if ((field.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) == 0
              && !field.isAnnotationPresent(AvroIgnore.class)) {
        Schema fieldSchema = createFieldSchema(field, names);
        AvroDefault defaultAnnotation
                = field.getAnnotation(AvroDefault.class);
        Object defaultValue = (defaultAnnotation == null)
                ? null
                : Json.parseJson(defaultAnnotation.value());
        AvroDoc annotatedDoc = field.getAnnotation(AvroDoc.class);    // Docstring
        String fdoc = (annotatedDoc != null) ? annotatedDoc.value() : null;

        if (defaultValue == null
                && fieldSchema.getType() == Schema.Type.UNION) {
          Schema defaultType = fieldSchema.getTypes().get(0);
          if (defaultType.getType() == Schema.Type.NULL) {
            defaultValue = JsonProperties.NULL_VALUE;
          }
        }
        AvroName annotatedName = field.getAnnotation(AvroName.class);       // Rename fields
        String fieldName = (annotatedName != null)
                ? annotatedName.value()
                : field.getName();
        Schema.Field recordField
                = new Schema.Field(fieldName, fieldSchema, fdoc, defaultValue);

        AvroMetas metas = field.getAnnotation(AvroMetas.class);
        if (metas != null) {
          for (AvroMeta meta : metas.value()) {
            recordField.addProp(meta.key(), meta.value());
          }
        }
        AvroMeta meta = field.getAnnotation(AvroMeta.class);              // add metadata
        if (meta != null) {
          recordField.addProp(meta.key(), meta.value());
        }
        for (Schema.Field f : fields) {
          if (f.name().equals(fieldName)) {
            throw new AvroTypeException("double field entry: " + fieldName);
          }
        }
        consumeFieldAlias(field, recordField);
        fields.add(recordField);
      }
    }
    if (error) { // add Throwable message
      fields.add(new Schema.Field("detailMessage", THROWABLE_MESSAGE, null, null));
    }
    schema.setFields(fields);
    AvroMetas metas = c.getAnnotation(AvroMetas.class);
    if (metas != null) {
      for (AvroMeta meta : metas.value()) {
        schema.addProp(meta.key(), meta.value());
      }
    }
    AvroMeta meta = c.getAnnotation(AvroMeta.class);
    if (meta != null) {
      schema.addProp(meta.key(), meta.value());
    }
    return schema;
  }

  @Override
  public String getSchemaName(final Object datum) {
    return super.getSchemaName(datum);
  }

  @Override
  public String toString() {
    return "ExtendedReflectData{" + "type2SchemaMap=" + type2SchemaMap + '}';
  }

}
