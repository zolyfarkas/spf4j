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
import java.util.Arrays;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.ImmutableDescriptor;
import javax.management.InvalidAttributeValueException;
import javax.management.JMRuntimeException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.spf4j.base.Throwables;

// We att the ex history to the message string, since the client is not required to have the exception classes
@SuppressFBWarnings("LEST_LOST_EXCEPTION_STACK_TRACE")
final class ExportedValuesMBean implements DynamicMBean {


  private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");

  private final Map<String, ExportedValue<?>> exportedValues;

  private final Map<String, ExportedOperation> exportedOperations;

  private final ObjectName objectName;

  private final MBeanInfo beanInfo;

  ExportedValuesMBean(final ObjectName objectName,
          final Map<String, ExportedValue<?>> exportedValues,
          final Map<String, ExportedOperation> exportedOperations) {
    this.exportedOperations = exportedOperations;
    this.exportedValues = exportedValues;
    this.objectName = objectName;
    this.beanInfo = createBeanInfo();
  }

  ExportedValuesMBean(final ObjectName objectName,
          final ExportedValue<?>[] exported, final ExportedOperation[] operations) {
    this.exportedValues = new HashMap<>(exported.length);
    for (ExportedValue<?> val : exported) {
      if (this.exportedValues.put(val.getName(), val) != null) {
        throw new IllegalArgumentException("Duplicate attribute " + val);
      }
    }
    this.exportedOperations = new HashMap<>(operations.length);
    for (ExportedOperation op : operations) {
      if (this.exportedOperations.put(op.getName(), op) != null) {
        throw new IllegalArgumentException("Duplicate operation " + op);
      }
    }
    this.objectName = objectName;
    this.beanInfo = createBeanInfo();
  }

  ExportedValuesMBean(final ExportedValuesMBean extend,
          final ExportedValue<?>[] exported, final ExportedOperation[] operations) {
    this.exportedValues = new HashMap<>(exported.length + extend.exportedValues.size());
    this.exportedValues.putAll(extend.exportedValues);
    for (ExportedValue<?> val : exported) {
      if (this.exportedValues.put(val.getName(), val) != null) {
        throw new IllegalArgumentException("Duplicate attribute " + val);
      }
    }
    this.exportedOperations = new HashMap<>(operations.length + extend.exportedOperations.size());
    this.exportedOperations.putAll(extend.exportedOperations);
    for (ExportedOperation op : operations) {
      if (this.exportedOperations.put(op.getName(), op) != null) {
        throw new IllegalArgumentException("Duplicate operation " + op);
      }
    }
    this.objectName = extend.getObjectName();
    this.beanInfo = extend.beanInfo;
  }

  ExportedValuesMBean(final ExportedValuesMBean extend,
          final Map<String, ExportedValue<?>> exportedValues,
          final Map<String, ExportedOperation> exportedOperations) {
    this.exportedValues = exportedValues;
    this.exportedValues.putAll(extend.exportedValues);
    this.exportedOperations = exportedOperations;
    this.exportedOperations.putAll(extend.exportedOperations);
    this.objectName = extend.getObjectName();
    this.beanInfo = createBeanInfo();
  }


  /**
   * @return - the object name of this mbean.
   */
  public ObjectName getObjectName() {
    return objectName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getAttribute(final String name) throws AttributeNotFoundException, MBeanException {
    ExportedValue<?> result = exportedValues.get(name);
    if (result == null) {
      throw new AttributeNotFoundException(name);
    }
    try {
      return result.get();
    } catch (OpenDataException | RuntimeException ex) {
      Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.SEVERE,
              "Exception while getting attr {0}", name);
      Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.SEVERE,
              "Exception detail", ex);
      throw new MBeanException(null, "Error getting attribute" + name + " detail:\n" + Throwables.toString(ex));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttribute(final Attribute attribute)
          throws AttributeNotFoundException, InvalidAttributeValueException {
    String name = attribute.getName();
    ExportedValue<Object> result = (ExportedValue<Object>) exportedValues.get(name);
    if (result == null) {
      throw new AttributeNotFoundException(name);
    }
    try {
      result.set(attribute.getValue());
    } catch (InvalidObjectException | RuntimeException ex) {
      Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.SEVERE,
              "Exception while setting attr {0}", attribute);
      Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.SEVERE,
              "Exception detail", ex);
      throw new InvalidAttributeValueException("Invalid value " + attribute
              + " detail:\n" + Throwables.toString(ex));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AttributeList getAttributes(final String[] names) {
    AttributeList list = new AttributeList(names.length);
    for (String name : names) {
      try {
        ExportedValue<?> attr = exportedValues.get(name);
        if (attr == null) {
          throw new IllegalArgumentException("No attribute with name " + name);
        }
        list.add(new Attribute(name, attr.get()));
      } catch (OpenDataException | RuntimeException ex) {
          Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.SEVERE,
                  "Exception getting attribute {0}", name);
          Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.SEVERE,
              "Exception detail", ex);
          throw new JMRuntimeException("Exception while getting attributes " + Arrays.toString(names) + ", detail:\n"
                  + Throwables.toString(ex));
      }
    }
    return list;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AttributeList setAttributes(final AttributeList list) {
    AttributeList result = new AttributeList(list.size());
    for (Attribute attr : list.asList()) {
      ExportedValue<Object> eval = (ExportedValue<Object>) exportedValues.get(attr.getName());
      if (eval != null) {
        try {
          eval.set(attr.getValue());
          result.add(attr);
        } catch (InvalidAttributeValueException | InvalidObjectException | RuntimeException ex) {
          Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.WARNING,
                  "Exception while setting attr {}", attr);
          Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.WARNING,
              "Exception detail", ex);
          throw new JMRuntimeException("Exception while setting attributes " + list + ", detail:\n"
                    + Throwables.toString(ex));
        }
      }
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object invoke(final String name, final Object[] args, final String[] sig) throws MBeanException {
    try {
      return exportedOperations.get(name).invoke(args);
    } catch (OpenDataException | InvalidObjectException | RuntimeException ex) {
      Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.WARNING,
              "Exception while invoking operation {0}({1})", new Object[] {name, args});
      Logger.getLogger(ExportedValuesMBean.class.getName()).log(Level.WARNING,
              "Exception detail", ex);
      throw new MBeanException(null, "Exception invoking" + name + " with " +  Arrays.toString(args) + ", detail:\n"
              + Throwables.toString(ex));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MBeanInfo getMBeanInfo() {
    return beanInfo;
  }

  public static ObjectName createObjectName(final String domain, final String name) {
    try {
      final String sanitizedDomain = INVALID_CHARS.matcher(domain).replaceAll("_");
      final String sanitizedName = INVALID_CHARS.matcher(name).replaceAll("_");
      StringBuilder builder = new StringBuilder(domain.length() + name.length() + 6);
      builder.append(sanitizedDomain).append(':');
      builder.append("name=").append(sanitizedName);
      return new ObjectName(builder.toString());
    } catch (MalformedObjectNameException e) {
      throw new IllegalArgumentException("Invalid name for " + domain + ", " + name, e);
    }
  }

  private MBeanInfo createBeanInfo() {
    MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[exportedValues.size()];
    int i = 0;
    for (ExportedValue<?> val : exportedValues.values()) {
      attrs[i++] = val.toAttributeInfo();
    }
    MBeanOperationInfo[] operations = new MBeanOperationInfo[exportedOperations.size()];
    i = 0;
    for (ExportedOperation op : exportedOperations.values()) {
      MBeanParameterInfo[] paramInfos = op.getParameterInfos();
      String description = op.getDescription();
      if (description == null || description.isEmpty()) {
        description = op.getName();
      }
      OpenType<?> openType = op.getReturnOpenType();
      operations[i++] = new MBeanOperationInfo(op.getName(), description, paramInfos,
              op.getReturnType().getName(), 0, openType == null ? null
                      : new ImmutableDescriptor(new String[]{"openType", "originalType"},
            new Object[]{openType, op.getReturnType().getName()}));
    }
    return new MBeanInfo(objectName.toString(), "spf4j exported",
            attrs, null, operations, null);
  }

  @Override
  public String toString() {
    return "ExportedValuesMBean{" + "exportedValues=" + exportedValues + ", exportedOperations="
            + exportedOperations + ", objectName=" + objectName + ", beanInfo=" + beanInfo + '}';
  }

}
