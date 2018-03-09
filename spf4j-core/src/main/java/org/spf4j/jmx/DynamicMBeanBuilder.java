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
package org.spf4j.jmx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.NotSerializableException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.management.ObjectName;
import org.spf4j.base.Reflections;
import org.spf4j.base.Strings;

/**
 * A JMX managed bean Builder.
 *
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class DynamicMBeanBuilder {

  private final Map<String, ExportedValue<?>> exportedAttributes;

  private final Map<String, ExportedOperation> exportedOps;

  public DynamicMBeanBuilder() {
    exportedAttributes = new HashMap<>(8);
    exportedOps = new HashMap<>(4);
  }

  public static DynamicMBeanBuilder newBuilder() {
    return new DynamicMBeanBuilder();
  }

  public DynamicMBeanBuilder withOperation(final ExportedOperation operation) {
    if (exportedOps.put(operation.getName(), operation) != null) {
       throw new IllegalArgumentException("Duplicate operation: " + operation);
    }
    return this;
  }

  public DynamicMBeanBuilder withAttribute(final ExportedValue<?> val) {
    if (exportedAttributes.put(val.getName(), val) != null) {
      throw new IllegalArgumentException("Duplicate attribute: " + val);
    }
    return this;
  }

  public DynamicMBeanBuilder withAttributes(final ExportedValue<?>... vals) {
    for (ExportedValue<?> ev : vals) {
      withAttribute(ev);
    }
    return this;
  }

  public DynamicMBeanBuilder withAttributes(final Map<String, Object> mapAttributes) {
    for (Map.Entry<String, Object> entry : mapAttributes.entrySet()) {
      String key = entry.getKey();
      if (key == null || key.isEmpty()) {
        continue; // do not exportAgg crap namep mapAttributes.
      }
      try {
        if (exportedAttributes.put(key, new MapExportedValue(mapAttributes, null, key, entry.getValue())) != null) {
          throw new IllegalArgumentException("Duplicate attribute: " + key);
        }
      } catch (NotSerializableException ex) {
        throw new IllegalArgumentException("Unable to export map entry via JMX: " + entry, ex);
      }
    }
    return this;
  }

  /**
   * add all exported (with @JmxExport) attributes and operations of the object.
   * @param object
   * @return
   */
  public DynamicMBeanBuilder withJmxExportObject(final Object object) {
    final Map<String, ExportedValue<?>> attrs = new HashMap<>(4);
    if (object instanceof Class) {
      for (Method method : ((Class<?>) object).getMethods()) {
        if (method.isSynthetic()) {
          continue;
        }
        if (Modifier.isStatic(method.getModifiers())) {
          JmxExport annot = method.getAnnotation(JmxExport.class);
          if (annot != null) {
            exportMethod(method, null, attrs, exportedOps, annot);
          }
        }
      }
    } else {
      final Class<? extends Object> oClass = object.getClass();
      for (Method method : oClass.getMethods()) {
        if (method.isSynthetic()) {
          continue;
        }
        JmxExport annot = method.getAnnotation(JmxExport.class);
        if (annot != null) {
          exportMethod(method, object, attrs, exportedOps, annot);
        }
      }
    }
    Collection<ExportedValue<?>> values = attrs.values();
    for (ExportedValue expVal : values) {
      if (!((BeanExportedValue) expVal).isValid()) {
        throw new IllegalArgumentException("If setter is exported, getter must be exported as well " + expVal);
      }
    }
    exportedAttributes.putAll(attrs);
    return this;
  }

  /**
   * add all exported (with @JmxExport) attributes and operations of the objects.
   * @param objects
   * @return
   */
  public DynamicMBeanBuilder withJmxExportObjects(final Object... objects) {
    for (Object object : objects) {
      withJmxExportObject(object);
    }
    return this;
  }

  /**
   * Build the dynamic mbean.
   * @param packageName
   * @param mbeanName
   * @return dynamic mbean or null if no exportable attributes or operations are present.
   */
  @Nullable
  public ExportedValuesMBean build(final String packageName, final String mbeanName) {
    if (exportedAttributes.isEmpty() && exportedOps.isEmpty()) {
      return null;
    }
    ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
    return new ExportedValuesMBean(objectName, exportedAttributes, exportedOps);
  }

  /**
   * Create a dynamic bean with extends the toExtend mbean with the attributes and operations from thsi builder.
   * @param toExtend
   * @return
   */
  @Nonnull
  public ExportedValuesMBean extend(@Nonnull final ExportedValuesMBean toExtend) {
    if (exportedAttributes.isEmpty() && exportedOps.isEmpty()) {
      return toExtend;
    }
    return new ExportedValuesMBean(toExtend, exportedAttributes, exportedOps);
  }


  /**
   * extend existing Mbean registered with provided packageName and mbeanName
   * @param packageName
   * @param mbeanName
   * @return null is nothing was registered.
   */
  @Nullable
  public ExportedValuesMBean extend(final String packageName, final String mbeanName) {
    if (exportedAttributes.isEmpty() && exportedOps.isEmpty()) {
      return null;
    }
    ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
    synchronized (Registry.class) {
      ExportedValuesMBean existing = (ExportedValuesMBean) Registry.unregister(objectName);
      ExportedValuesMBean mbean;
      if (existing == null) {
        mbean = new ExportedValuesMBean(objectName, exportedAttributes, exportedOps);
      } else {
        mbean = new ExportedValuesMBean(existing, exportedAttributes, exportedOps);
      }
      Registry.registerMBean(objectName, mbean);
      return mbean;
    }

  }

  public boolean isEmpty() {
    return exportedAttributes.isEmpty() && exportedOps.isEmpty();
  }

  /**
   * Replace mbean registered with packageName and mbeanName with a mbean constructed by this builder.
   * @param packageName
   * @param mbeanName
   * @return the MBean that was registered.
   */
  @Nonnull
  public ExportedValuesMBean replace(final String packageName, final String mbeanName) {
    if (isEmpty()) {
      throw new IllegalArgumentException("Nothing to register " + this);
    }
    ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
    ExportedValuesMBean mbean = new ExportedValuesMBean(objectName, exportedAttributes, exportedOps);
    Registry.registerMBean(objectName, mbean);
    return mbean;
  }

  @Nullable
  public ExportedValuesMBean replaceIfExports(final String packageName, final String mbeanName) {
    if (isEmpty()) {
      return null;
    }
    ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
    ExportedValuesMBean mbean = new ExportedValuesMBean(objectName, exportedAttributes, exportedOps);
    Registry.registerMBean(objectName, mbean);
    return mbean;
  }


  /**
   * register a mbean.
   * @param packageName
   * @param mbeanName
   * @return  and a dinamic bean instance that was registered otherwise.
   * @throws InstanceAlreadyExistsException is a instance already exists.
   */
  @Nonnull
  public ExportedValuesMBean register(final String packageName, final String mbeanName) {
    if (isEmpty()) {
      throw new IllegalArgumentException("Nothing to register " + this);
    }
    ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
    ExportedValuesMBean mbean = new ExportedValuesMBean(objectName, exportedAttributes, exportedOps);
    Registry.registerIfNotExistsMBean(objectName, mbean);
    return mbean;
  }


  @Nullable
  public ExportedValuesMBean registerIfExports(final String packageName, final String mbeanName) {
    if (isEmpty()) {
      return null;
    }
    ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
    ExportedValuesMBean mbean = new ExportedValuesMBean(objectName, exportedAttributes, exportedOps);
    Registry.registerIfNotExistsMBean(objectName, mbean);
    return mbean;
  }



static void exportMethod(final Method method,
          @Nullable final Object object, final Map<String, ExportedValue<?>> exportedAttributes,
          final Map<String, ExportedOperation> exportedOps, final JmxExport annot) {
    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
      method.setAccessible(true);
      return null;
    });
    String methodName = method.getName();
    int nrParams = method.getParameterCount();
    if (nrParams == 0 && methodName.startsWith("get")) {
      String valueName = methodName.substring("get".length());
      valueName = Strings.withFirstCharLower(valueName);
      addGetter(valueName, exportedAttributes, annot, method, object);
    } else if (nrParams == 0 && methodName.startsWith("is")) {
      String valueName = methodName.substring("is".length());
      valueName = Strings.withFirstCharLower(valueName);
      addGetter(valueName, exportedAttributes, annot, method, object);
    } else if (nrParams == 1 && methodName.startsWith("set")) {
      addSetter(methodName, exportedAttributes, method, object, annot);
    } else {
      String opName = methodName;
      String nameOverwrite = annot.value();
      if (!"".equals(nameOverwrite)) {
        opName = nameOverwrite;
      }
      ExportedOperation existing = exportedOps.put(opName, new ExportedOperationImpl(opName,
              annot.description(), method, object, annot.mapOpenType()));
      if (existing != null) {
        throw new IllegalArgumentException("exporting operations with same name not supported: " + opName);
      }
    }
  }

  private static void addSetter(final String methodName, final Map<String, ExportedValue<?>> exportedAttributes,
          final Method method, final Object object, final JmxExport annot) {
    String customName = annot.value();
    String valueName;
    if ("".equals(customName)) {
      valueName = methodName.substring("set".length());
      valueName = Strings.withFirstCharLower(valueName);
    } else {
      valueName = customName;
    }

    BeanExportedValue existing = (BeanExportedValue) exportedAttributes.get(valueName);
    Class<?> parameterType = Reflections.getParameterTypes(method)[0];
    if (existing == null) {
      existing = new BeanExportedValue(valueName, null,
              null, method, object, parameterType, annot.mapOpenType());
    } else {
      if (existing.getValueType() != parameterType) {
        throw new IllegalArgumentException(
                "Getter and setter icorrectly defined " + existing + ' ' + method);
      }
      existing = existing.withSetter(method);
    }
    exportedAttributes.put(valueName, existing);
  }

  private static void addGetter(final String pvalueName,
          final Map<String, ExportedValue<?>> exported,
          final JmxExport annot, final Method method, final Object object) {
    String customName = annot.value();
    String valueName = "".equals(customName) ? pvalueName : customName;
    BeanExportedValue existing = (BeanExportedValue) exported.get(valueName);
    if (existing == null) {
      existing = new BeanExportedValue(
              valueName, annot.description(),
              method, null, object, method.getGenericReturnType(), annot.mapOpenType());
    } else {
      if (existing.getValueType() != method.getReturnType()) {
        throw new IllegalArgumentException(
                "Getter and setter icorrectly defined " + existing + ' ' + method);
      }
      existing = existing.withGetter(method, annot.description());
    }
    exported.put(valueName, existing);
  }

  @Override
  public String toString() {
    return "MBeanBuilder{" + "exportedAttributes=" + exportedAttributes
            + ", exportedOps=" + exportedOps + '}';
  }



}
