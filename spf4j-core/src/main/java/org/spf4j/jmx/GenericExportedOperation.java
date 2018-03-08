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

import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.lang.reflect.Type;
import java.util.Arrays;
import javax.annotation.Nullable;
import javax.management.MBeanParameterInfo;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import org.spf4j.base.Invocation;

/**
 *
 * @author zoly
 */
public final class GenericExportedOperation implements ExportedOperation {

  private final String name;

  private final String description;

  private final Invocation invocation;

  private final MBeanParameterInfo[] paramInfos;

  private final JMXBeanMapping[] argConverters;

  private final JMXBeanMapping resultConverter;

  private final Type returnType;

  public GenericExportedOperation(final String name, final String description,
          final Invocation invocation, final Type[] parameterTypes, final Type returnType,
          final String[] parameterNames, final String[] parameterDescriptions,
          final boolean[] mapOpenTypeParam,
          final boolean mapOpenType) {
    this.name = name;
    this.description = description;
    this.invocation = invocation;
    this.returnType = returnType;
    if (mapOpenType) {
      try {
        resultConverter = GlobalMXBeanMapperSupplier.getOpenTypeMapping(returnType);
      } catch (NotSerializableException ex) {
        throw new UnsupportedOperationException("Cannot export " + name
                + " returned type " + returnType + "  not serializable", ex);
      }
    } else {
      resultConverter = null;
    }
    argConverters = new JMXBeanMapping[parameterTypes.length];
    paramInfos = new MBeanParameterInfo[parameterTypes.length];
    for (int i = 0; i < paramInfos.length; i++) {
      String pname = parameterNames[i];
      String pdesc = parameterDescriptions[i];
      boolean mapParamOpenType = mapOpenTypeParam[i];
      if ("".equals(pname)) {
        pname = "param_" + i;
      }
      if (pdesc.isEmpty()) {
        pdesc = name;
      }
      Type parameterType = parameterTypes[i];

      JMXBeanMapping paramOTM;
      if (mapParamOpenType) {
        try {
          paramOTM = GlobalMXBeanMapperSupplier.getOpenTypeMapping(parameterType);
        } catch (NotSerializableException ex) {
          throw new UnsupportedOperationException("Cannot export "
                  + name + " arg " + i + " not serializable from " + Arrays.toString(parameterTypes), ex);
        }
      } else {
        paramOTM = null;
      }
      if (paramOTM != null) {
        paramInfos[i] = new OpenMBeanParameterInfoSupport(pname, pdesc, paramOTM.getOpenType());
        argConverters[i] = paramOTM;
      } else {
        paramInfos[i] = new MBeanParameterInfo(pname, parameterType.getTypeName(), pdesc);
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
    for (int i = 0; i < parameters.length; i++) {
      JMXBeanMapping argConverter = argConverters[i];
      if (argConverter != null) {
        parameters[i] = argConverter.fromOpenValue(parameters[i]);
      }
    }
    try {
      Object rVal = invocation.invoke(parameters);
      if (resultConverter != null) {
        return resultConverter.toOpenValue(rVal);
      } else {
        return rVal;
      }
    } catch (Exception ex) {
      OpenDataException x
              = new OpenDataException("Cannot invoke " + invocation + " with " + Arrays.toString(parameters));
      x.addSuppressed(ex);
      throw x;
    }
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public MBeanParameterInfo[] getParameterInfos() {
    return paramInfos;
  }

  @Override
  public Class<?> getReturnType() {
    return TypeToken.of(this.returnType).getRawType();
  }

  @Override
  @Nullable
  public OpenType<?> getReturnOpenType() {
    return (resultConverter != null) ? resultConverter.getOpenType() : null;
  }

  @Override
  public String toString() {
    return "GenericExportedOperation{" + "name=" + name + ", description=" + description
            + ", invocation=" + invocation + ", paramInfos=" + Arrays.toString(paramInfos) + ", argConverters="
            + Arrays.toString(argConverters) + ", resultConverter=" + resultConverter
            + ", returnType=" + returnType + '}';
  }

}
