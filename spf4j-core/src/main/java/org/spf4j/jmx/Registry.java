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
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * Utility class that allows to easily exportAgg via JMX java beans.
 *
 * attributes can be exported as simply as:
 *
 *       @JmxExport
 *       public String [][] getMatrix() {
 *           return matrix.clone();
 *       }
 *
 * for a writable attribute you will need to annotate the setter as well.
 *
 * Operations are as simple as:
 *
 *       @JmxExport(description = "test operation")
 *       public String doStuff(@JmxExport(value = "what", description = "some param") final String what,
 *               final String where) {
 *           return "Doing " + what + " " + where;
 *       }
 *
 * A object annotated as above can be exported via JMX as simple as Registry.export(object).
 *
 * The registry utility also allows you to export the content of a Map as JMX attributes:
 *
 * Registry.export("package", "beanName", map);
 *
 * OpenType conversions are made for all type where this is doable.
 * Avro SpecificRecord's are converted to CompositeData Open type.
 * OpenType conversions can be enabled/disabled with JmcExport annotation.
 *
 *
 * @author Zoltan Farkas
 */

public final class Registry {

  private static final MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();

  private static final Map<ObjectName, Object> REGISTERED = new HashMap<>();

  private Registry() {
  }

  /**
   * Register MBean, will replace any existing bean.
   * @param objectName
   * @param mbean
   * @return
   */
  public static synchronized Object registerMBean(final ObjectName objectName, final Object mbean) {
    Object replaced = null;
    if (MBEAN_SERVER.isRegistered(objectName)) {
      try {
        replaced = REGISTERED.remove(objectName);
        MBEAN_SERVER.unregisterMBean(objectName);
      } catch (InstanceNotFoundException | MBeanRegistrationException ex) {
        throw new IllegalStateException(ex);
      }
    }
    try {
      MBEAN_SERVER.registerMBean(mbean, objectName);
    } catch (InstanceAlreadyExistsException  ex) {
      throw new IllegalStateException(ex);
    } catch (NotCompliantMBeanException | MBeanRegistrationException ex) {
      throw new IllegalArgumentException("Invalid MBean " + mbean, ex);
    }
    REGISTERED.put(objectName, mbean);
    return replaced;
  }

  public static synchronized void registerIfNotExistsMBean(final ObjectName objectName, final Object mbean) {
    try {
      MBEAN_SERVER.registerMBean(mbean, objectName);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException ex) {
      throw new IllegalArgumentException(ex);
    }
    REGISTERED.put(objectName, mbean);
  }

  public static synchronized Object getRegistered(final ObjectName objectName) {
    return REGISTERED.get(objectName);
  }

  public static Object getRegistered(final String domain, final String name) {
    return getRegistered(ExportedValuesMBean.createObjectName(domain, name));
  }

  public static Object registerMBean(final String domain, final String name, final Object object) {
    return Registry.registerMBean(ExportedValuesMBean.createObjectName(domain, name), object);
  }

  @Nullable
  public static Object unregister(final Object object) {
    final Class<? extends Object> aClass = object.getClass();
    return unregister(aClass.getPackage().getName(), aClass.getSimpleName());
  }

  @Nullable
  public static Object unregister(final Class<?> object) {
    return unregister(object.getPackage().getName(), object.getSimpleName());
  }

  @Nullable
  public static Object unregister(final String packageName, final String mbeanName) {
    ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
    return unregister(objectName);
  }

  @Nullable
  public static synchronized Object unregister(final ObjectName objectName) {
    Object result = null;
    if (MBEAN_SERVER.isRegistered(objectName)) {
      try {
        result = REGISTERED.remove(objectName);
        MBEAN_SERVER.unregisterMBean(objectName);
      } catch (InstanceNotFoundException | MBeanRegistrationException ex) {
        throw new IllegalArgumentException("Cannot unregister " + objectName, ex);
      }
    }
    return result;
  }

  public static ExportedValuesMBean export(final Object object) {
    final Class<? extends Object> aClass = object.getClass();
    return export(aClass.getPackage().getName(), aClass.getSimpleName(), object);
  }

  public static ExportedValuesMBean export(final Class<?> object) {
    return export(object.getPackage().getName(), object.getSimpleName(), object);
  }

  public static ExportedValuesMBean export(final String packageName, final String mbeanName,
          final Object... objects) {
    return new DynamicMBeanBuilder().withJmxExportObjects(objects).replace(packageName, mbeanName);
  }

  @Nullable
  public static ExportedValuesMBean exportIfNeeded(final String packageName, final String mbeanName,
          final Object... objects) {
    return new DynamicMBeanBuilder().withJmxExportObjects(objects).replaceIfExports(packageName, mbeanName);
  }

  /**
   * @deprecated use DynamicMBeanBuilder instead.
   */
  @SuppressFBWarnings("OCP_OVERLY_CONCRETE_PARAMETER")
  @Deprecated
  public static ExportedValuesMBean export(final String packageName, final String mbeanName,
          final Properties attributes, final Object... objects) {
    DynamicMBeanBuilder builder = new DynamicMBeanBuilder().withJmxExportObjects(objects);
    if (attributes != null) {
       builder.withAttributes((Map) attributes);
    }
    return builder.replace(packageName, mbeanName);
  }

  /**
   * @deprecated use BynamicMBeanBuilder instead.
   */
  @Deprecated
  public static ExportedValuesMBean export(final String packageName, final String mbeanName,
          @Nullable final Map<String, Object> attributes, final Object... objects) {
    DynamicMBeanBuilder builder = new DynamicMBeanBuilder().withJmxExportObjects(objects);
    if (attributes != null) {
       builder.withAttributes(attributes);
    }
    return builder.replace(packageName, mbeanName);
  }
}
