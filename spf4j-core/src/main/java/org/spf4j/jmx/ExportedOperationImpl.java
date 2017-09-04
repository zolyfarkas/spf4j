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

import com.sun.jmx.mbeanserver.MXBeanMapping;
import java.io.InvalidObjectException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.management.MBeanParameterInfo;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import org.spf4j.base.Reflections;

/**
 *
 * @author zoly
 */
final class ExportedOperationImpl implements ExportedOperation {

  private final String name;

  private final String description;

  private final Method method;

  private final Object object;

  private final MBeanParameterInfo[] paramInfos;

  private final MXBeanMapping[] argConverters;

  private final MXBeanMapping resultConverter;

  ExportedOperationImpl(final String name, final String description,
          final Method method, final Object object) {
    this.name = name;
    this.description = description;
    this.method = method;
    this.object = object;
    Class<?>[] parameterTypes = Reflections.getParameterTypes(method);
    Class<?> returnType = method.getReturnType();
    resultConverter = OpenTypeConverter.getMXBeanMapping(returnType);
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    argConverters = new MXBeanMapping[parameterTypes.length];
    paramInfos = new MBeanParameterInfo[parameterTypes.length];
    for (int i = 0; i < paramInfos.length; i++) {
      Annotation[] annotations = parameterAnnotations[i];
      String pname = "";
      String pdesc = "";
      for (Annotation annot : annotations) {
        if (annot.annotationType() == JmxExport.class) {
          JmxExport eAnn = (JmxExport) annot;
          pname = eAnn.value();
          pdesc = eAnn.description();
          break;
        }
      }
      if ("".equals(pname)) {
        pname = "param_" + i;
      }
      if (pdesc.isEmpty()) {
        pdesc = name;
      }
      Class<?> parameterType = parameterTypes[i];
      OpenType<?> openType = OpenTypeConverter.getOpenType(parameterType);
      if (openType != null) {
        paramInfos[i] = new OpenMBeanParameterInfoSupport(pname, pdesc, openType);
        argConverters[i] = OpenTypeConverter.getMXBeanMapping(parameterType);
      } else {
        paramInfos[i] = new MBeanParameterInfo(pname, parameterType.getName(), pdesc);
        argConverters[i] = null;
      }
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Object invoke(final Object[] parameters) throws OpenDataException, InvalidObjectException {
    try {
      for (int i = 0; i < parameters.length; i++) {
        MXBeanMapping argConverter = argConverters[i];
        if (argConverter != null) {
          parameters[i] = argConverter.fromOpenValue(parameters[i]);
        }
      }
      Object rVal = method.invoke(object, parameters);
      if (resultConverter != null) {
        return resultConverter.toOpenValue(rVal);
      } else {
        return rVal;
      }
    } catch (IllegalAccessException | InvocationTargetException ex) {
      OpenDataException x
              = new OpenDataException("Cannot invoke " + method + " with " + Arrays.toString(parameters));
      x.addSuppressed(ex);
      throw x;
    }
  }

  @Override
  public MBeanParameterInfo[] getParameterInfos() {
    return paramInfos;
  }

  @Override
  public Class<?> getReturnType() {
    return method.getReturnType();
  }

  @Override
  public String toString() {
    return "ExportedOperationImpl{" + "name=" + name + ", description=" + description
            + ", method=" + method + ", object=" + object + ", paramInfos=" + Arrays.toString(paramInfos) + '}';
  }

}
