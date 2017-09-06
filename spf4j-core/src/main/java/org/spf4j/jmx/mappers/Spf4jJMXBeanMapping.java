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
package org.spf4j.jmx.mappers;

import com.sun.jmx.mbeanserver.MXBeanMapping;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import static javax.management.openmbean.SimpleType.STRING;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.apache.avro.specific.SpecificRecordBase;
import org.spf4j.base.Throwables;
import org.spf4j.jmx.JMXBeanMapping;
import org.spf4j.jmx.JMXBeanMappingSupplier;

/**
 * Mapping class to improve open type mappings for collections and avro objects.
 * @author Zoltan Farkas
 */
@SuppressWarnings({"unchecked", "checkstyle:visibilitymodifier"})
abstract class Spf4jJMXBeanMapping implements JMXBeanMapping {


  static final OpenType<?> IN_PROGRESS;

  static {
    OpenType<?> t;
    try {
      t = new InProgress();
    } catch (OpenDataException e) {
      // Should not reach here
      throw new AssertionError(e);
    }
    IN_PROGRESS = t;
  }

  private static final String KEY = "key";
  private static final String VALUE = "value";
  private static final String[] MAP_INDEX_NAMES = {KEY};
  private static final String[] MAP_ITEM_NAMES = {KEY, VALUE};
  private static final Type[] STR_STR_TYPES = {String.class, String.class};


  private boolean isBasicType;
  protected OpenType<?> openType;
  protected Class<?> mappedTypeClass;

  Spf4jJMXBeanMapping() {
    this(false, IN_PROGRESS, null);
  }

  Spf4jJMXBeanMapping(final boolean isBasicType, final OpenType<?> openType, final Class<?> mappedTypeClass) {
    this.isBasicType = isBasicType;
    this.openType = openType;
    this.mappedTypeClass = mappedTypeClass;
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  static JMXBeanMapping newMappedType(final Type javaType, final JMXBeanMappingSupplier mappings)
          throws OpenDataException, NotSerializableException {

    JMXBeanMapping mt = null;
    if (javaType instanceof Class) {
      final Class<?> c = (Class<?>) javaType;
      if (c.isEnum()) {
        mt = new EnumMXBeanType(c);
      } else if (c.isArray()) {
        mt = new ArrayMXBeanType(c, mappings);
      } else if (SpecificRecordBase.class.isAssignableFrom(c)) {
        mt = new SpecificRecordOpenTypeMapping((Class) c, mappings);
      } else if (Properties.class.isAssignableFrom(c)) {
        mt = new MapMXBeanType(c, mappings);
      } else  {
        //falling back to MXBeanMappingFactory.DEFAULT
        try {
        MXBeanMapping mapping = MXBeanMappingFactory.DEFAULT.mappingForType(javaType, new MXBeanMappingFactory() {
          @Override
          public MXBeanMapping mappingForType(final Type t, final MXBeanMappingFactory f) throws OpenDataException {
            JMXBeanMapping m;
            try {
              m = mappings.get(t);
            } catch (NotSerializableException ex) {
              OpenDataException tex = new OpenDataException(t + " is not seriablizable ");
              tex.initCause(ex);
              throw tex;
            }
            if (m != null) {
              return MXBeanMappings.convert(m);
            }
            return MXBeanMappingFactory.DEFAULT.mappingForType(t, f);
          }
        });
        mt = MXBeanMappings.convert(mapping);
        } catch (OpenDataException ex) {
          NotSerializableException nsex = Throwables.first(ex, NotSerializableException.class);
          if (nsex != null) {
            throw nsex;
          } else {
            throw ex;
          }
        }
      }
    } else if (javaType instanceof ParameterizedType) {
      final ParameterizedType pt = (ParameterizedType) javaType;
      final Type rawType = pt.getRawType();
      if (rawType instanceof Class) {
        final Class<?> rc = (Class<?>) rawType;
        if (Collection.class.isAssignableFrom(rc)) {
          mt = new ListMXBeanType(pt, mappings);
        } else if (Map.class.isAssignableFrom(rc)) {
          mt = new MapMXBeanType(pt, mappings);
        }
      }
    } else if (javaType instanceof GenericArrayType) {
      final GenericArrayType t = (GenericArrayType) javaType;
      mt = new GenericArrayMXBeanType(t, mappings);
    }
    return mt;
  }

  // basic types do not require data mapping
  static Spf4jJMXBeanMapping newBasicType(final Class<?> c, final OpenType<?> ot)
          throws OpenDataException {
    return new Spf4jJMXBeanMapping.BasicMXBeanType(c, ot);
  }

  // Return the mapped open type
  @Override
  public OpenType<?> getOpenType() {
    return openType;
  }

  boolean isBasicType() {
    return isBasicType;
  }

  @Override
  public Class<?> getMappedType() {
    return mappedTypeClass;
  }

  /**
   * Basic Types - Classes that do not require data conversion
   *              including primitive types and all SimpleType
   *
   *   Mapped open type: SimpleType for corresponding basic type
   *
   * Data Mapping:
   *   T <-> T (no conversion)
   **/
  private static class BasicMXBeanType extends Spf4jJMXBeanMapping {

    BasicMXBeanType(final Class<?> c, final OpenType<?> openType) {
      super(true, openType, c);
    }

    @Override
    public Type getJavaType() {
      return getMappedType();
    }

    @Override
    public Object toOpenValue(final Object data) {
      return data;
    }

    @Override
    public Object fromOpenValue(final Object data) {
      return data;
    }
  }

  /**
   * Enum subclasses
   *   Mapped open type - String
   *
   * Data Mapping:
   *   Enum <-> enum's name
   */
  private static class EnumMXBeanType extends Spf4jJMXBeanMapping {

    final Class enumClass;

    EnumMXBeanType(final Class<?> c) {
      super(false, STRING, String.class);
      this.enumClass = c;
    }

    @Override
    public Type getJavaType() {
      return enumClass;
    }

    @Override
    public Object toOpenValue(final Object data) {
      return ((Enum) data).name();
    }

    @Override
    public Object fromOpenValue(final Object data)
            throws InvalidObjectException {

      try {
        return Enum.valueOf(enumClass, (String) data);
      } catch (IllegalArgumentException e) {
        // missing enum constants
        final InvalidObjectException ioe
                = new InvalidObjectException("Enum constant named "
                        + (String) data + " is missing");
        ioe.initCause(e);
        throw ioe;
      }
    }
  }

  /**
   * Array E[]
   *   Mapped open type - Array with element of OpenType for E
   *
   * Data Mapping:
   *   E[] <-> openTypeData(E)[]
   */
  private static class ArrayMXBeanType extends Spf4jJMXBeanMapping {

    private final Class<?> arrayClass;
    protected JMXBeanMapping componentType;
    protected JMXBeanMapping baseElementType;

    ArrayMXBeanType(final Class<?> c, final JMXBeanMappingSupplier mappings)
            throws OpenDataException, NotSerializableException {
      this.arrayClass = c;
      this.componentType = mappings.get(c.getComponentType());

      StringBuilder className = new StringBuilder();
      Class<?> et = c;
      int dim;
      for (dim = 0; et.isArray(); dim++) {
        className.append('[');
        et = et.getComponentType();
      }
      baseElementType = mappings.get(et);
      if (et.isPrimitive()) {
        className = new StringBuilder(c.getName());
      } else {
        className.append('L').append(baseElementType.getMappedType().getTypeName()).append(';');
      }
      try {
        mappedTypeClass = Class.forName(className.toString());
      } catch (ClassNotFoundException e) {
        final OpenDataException ode
                = new OpenDataException("Cannot obtain array class " + className);
        ode.initCause(e);
        throw ode;
      }

      openType = new ArrayType<>(dim, baseElementType.getOpenType());
    }

    protected ArrayMXBeanType() {
      arrayClass = null;
    }

    public Type getJavaType() {
      return arrayClass;
    }

    @Override
    @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
    public Object toOpenValue(final Object data) throws OpenDataException {
      if (baseElementType.isSimpleType()) {
        return data;
      }
      final Object[] array = (Object[]) data;
      final Object[] openArray = (Object[]) Array.newInstance(componentType.getMappedType(),
              array.length);
      int i = 0;
      for (Object o : array) {
        if (o == null) {
          openArray[i] = null;
        } else {
          openArray[i] = componentType.toOpenValue(o);
        }
        i++;
      }
      return openArray;
    }

    @Override
    @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
    public Object fromOpenValue(final Object data)
            throws InvalidObjectException {
      if (baseElementType.isSimpleType()) {
        return data;
      }

      final Object[] openArray = (Object[]) data;
      final Object[] array = (Object[]) Array.newInstance((Class) componentType.getJavaType(),
              openArray.length);
      int i = 0;
      for (Object o : openArray) {
        if (o == null) {
          array[i] = null;
        } else {
          array[i] = componentType.fromOpenValue(o);
        }
        i++;
      }
      return array;
    }

  }

  private static class GenericArrayMXBeanType extends ArrayMXBeanType {

    private final GenericArrayType gtype;

    GenericArrayMXBeanType(final GenericArrayType gat, final JMXBeanMappingSupplier mappings)
            throws OpenDataException, NotSerializableException {
      this.gtype = gat;
      this.componentType = mappings.get(gat.getGenericComponentType());

      StringBuilder className = new StringBuilder();
      Type elementType = gat;
      int dim;
      for (dim = 0; elementType instanceof GenericArrayType; dim++) {
        className.append('[');
        GenericArrayType et = (GenericArrayType) elementType;
        elementType = et.getGenericComponentType();
      }
      baseElementType = mappings.get(elementType);
      if (elementType instanceof Class && ((Class) elementType).isPrimitive()) {
        className = new StringBuilder(gat.toString());
      } else {
        className.append('L').append(baseElementType.getMappedType().getTypeName()).append(';');
      }
      try {
        mappedTypeClass = Class.forName(className.toString());
      } catch (ClassNotFoundException e) {
        final OpenDataException ode
                = new OpenDataException("Cannot obtain array class " + className);
        ode.initCause(e);
        throw ode;
      }

      openType = new ArrayType<>(dim, baseElementType.getOpenType());
    }

    @Override
    public Type getJavaType() {
      return gtype;
    }

  }

  /**
   * List<E>
   *   Mapped open type - Array with element of OpenType for E
   *
   * Data Mapping:
   *   List<E> <-> openTypeData(E)[]
   */
  private static class ListMXBeanType extends Spf4jJMXBeanMapping {

    private final ParameterizedType javaType;
    private final JMXBeanMapping paramType;
    private final Class<?> rawType;

    ListMXBeanType(final ParameterizedType pt, final JMXBeanMappingSupplier mappings)
            throws OpenDataException, NotSerializableException {
      this.javaType = pt;
      this.rawType = (Class) pt.getRawType();
      final Type[] argTypes = pt.getActualTypeArguments();
      assert (argTypes.length == 1);

      if (!(argTypes[0] instanceof Class)) {
        throw new OpenDataException("Element Type for " + pt
                + " not supported");
      }
      final Class<?> et = (Class<?>) argTypes[0];
      if (et.isArray()) {
        throw new OpenDataException("Element Type for " + pt
                + " not supported");
      }
      paramType = mappings.get(et);
      String cname = "[L" + paramType.getMappedType().getName() + ";";
      try {
        mappedTypeClass = Class.forName(cname);
      } catch (ClassNotFoundException e) {
        final OpenDataException ode
                = new OpenDataException("Array class not found " + cname);
        ode.initCause(e);
        throw ode;
      }
      openType = new ArrayType<>(1, paramType.getOpenType());
    }

    @Override
    public Type getJavaType() {
      return javaType;
    }

    @Override
    public Object toOpenValue(final Object data) throws OpenDataException {
      final Collection<Object> list = (Collection<Object>) data;

      final Object[] openArray = (Object[]) Array.newInstance(paramType.getMappedType(),
              list.size());
      int i = 0;
      for (Object o : list) {
        openArray[i++] = paramType.toOpenValue(o);
      }
      return openArray;
    }

    @Override
    public Object fromOpenValue(final Object data)
            throws InvalidObjectException {

      final Object[] openArray = (Object[]) data;
      Collection<Object> result;
      if (rawType.isInterface()) {
        if (Set.class == rawType) {
          result = new HashSet<>(openArray.length);
        } else if (Deque.class == rawType) {
          result = new ArrayDeque<>(openArray.length);
        } else {
          result = new ArrayList<>(openArray.length);
        }
      } else {
        try {
          result = (Collection) rawType.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
          InvalidObjectException iox = new InvalidObjectException("Cannot instantiate " + rawType);
          iox.addSuppressed(ex);
          throw iox;
        }
      }
      for (Object o : openArray) {
        result.add(paramType.fromOpenValue(o));
      }
      return result;
    }
  }

  /**
   * Map<K,V>
   *   Mapped open type - TabularType with row type:
   *                        CompositeType:
   *                          "key"   of openDataType(K)
   *                          "value" of openDataType(V)
   *                        "key" is the index name
   *
   * Data Mapping:
   *   Map<K,V> <-> TabularData
   */
  private static class MapMXBeanType extends Spf4jJMXBeanMapping {

    private final Type javaType;
    private final Class<?> rawType;
    private final JMXBeanMapping keyType;
    private final JMXBeanMapping valueType;

    @SuppressFBWarnings({ "CLI_CONSTANT_LIST_INDEX", "ITC_INHERITANCE_TYPE_CHECKING" })
    MapMXBeanType(final Type pt, final JMXBeanMappingSupplier mappings)
      throws OpenDataException, NotSerializableException {
      this.javaType = pt;

      final Type[] argTypes;
      if (pt instanceof ParameterizedType) {
         argTypes = ((ParameterizedType) pt).getActualTypeArguments();
         rawType = (Class) ((ParameterizedType) pt).getRawType();
      } else if (pt instanceof Class && Properties.class.isAssignableFrom((Class) pt)) {
        argTypes = STR_STR_TYPES;
        rawType = (Class) pt;
      } else {
        throw new OpenDataException("Unsupported type " + pt);
      }
      assert (argTypes.length == 2);
      this.keyType = mappings.get(argTypes[0]);
      this.valueType = mappings.get(argTypes[1]);

      // FIXME: generate typeName for generic
      String typeName = pt.getTypeName();
      final OpenType<?>[] mapItemTypes = new OpenType<?>[]{
        keyType.getOpenType(),
        valueType.getOpenType()
      };
      final CompositeType rowType
              = new CompositeType(typeName,
                      typeName,
                      MAP_ITEM_NAMES,
                      MAP_ITEM_NAMES,
                      mapItemTypes);

      openType = new TabularType(typeName, typeName, rowType, MAP_INDEX_NAMES);
      mappedTypeClass = javax.management.openmbean.TabularData.class;
    }

    @Override
    public Type getJavaType() {
      return javaType;
    }

    @Override
    public Object toOpenValue(final Object data) throws OpenDataException {
      final Map<Object, Object> map = (Map<Object, Object>) data;
      final TabularType tabularType = (TabularType) openType;
      final TabularData table = new TabularDataSupport(tabularType);
      final CompositeType rowType = tabularType.getRowType();

      for (Map.Entry<Object, Object> entry : map.entrySet()) {
        final Object key = keyType.toOpenValue(entry.getKey());
        final Object value = valueType.toOpenValue(entry.getValue());
        final CompositeData row
                = new CompositeDataSupport(rowType,
                        MAP_ITEM_NAMES,
                        new Object[]{key, value});
        table.put(row);
      }
      return table;
    }

    @Override
    public Object fromOpenValue(final Object data)
            throws InvalidObjectException {

      final TabularData td = (TabularData) data;
      Map<Object, Object> result;
      if (rawType.isInterface()) {
        result = new HashMap<>();
      } else {
        try {
          result = (Map) rawType.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
          InvalidObjectException iox = new InvalidObjectException("Cannot instantiate " + rawType);
          iox.addSuppressed(ex);
          throw iox;
        }
      }
      for (CompositeData row : (Collection<CompositeData>) td.values()) {
        Object key = keyType.fromOpenValue(row.get(KEY));
        Object value = valueType.fromOpenValue(row.get(VALUE));
        result.put(key, value);
      }
      return result;
    }
  }

  private static final class InProgress extends OpenType {

    private static final long serialVersionUID = 1L;

    private static final String DESCRIPTION
            = "Marker to detect recursive type use -- internal use only!";

    InProgress() throws OpenDataException {
      super("java.lang.String", "java.lang.String", DESCRIPTION);
    }

    public String toString() {
      return DESCRIPTION;
    }

    public int hashCode() {
      return 0;
    }

    public boolean isValue(final Object o) {
      return false;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      return this.getClass() == obj.getClass();
    }
  }


}
