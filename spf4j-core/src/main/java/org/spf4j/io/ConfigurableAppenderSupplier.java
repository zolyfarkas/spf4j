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
package org.spf4j.io;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.reflect.CachingTypeMapWrapper;
import org.spf4j.reflect.GraphTypeMap;

/**
 *
 * @author zoly
 */
@Beta
@ParametersAreNonnullByDefault
public final class ConfigurableAppenderSupplier implements ObjectAppenderSupplier {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurableAppenderSupplier.class);


  private final CachingTypeMapWrapper<ObjectAppender> appenderMap;

  public ConfigurableAppenderSupplier() {
    this(true, x -> false);
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public ConfigurableAppenderSupplier(final boolean registerFromServiceLoader, final Predicate<Class<?>> except,
          final ObjectAppender<?>... appenders) {
    appenderMap = new CachingTypeMapWrapper<>(new GraphTypeMap());
    if (registerFromServiceLoader) {
      @SuppressWarnings("unchecked")
      ServiceLoader<ObjectAppender<?>> load = (ServiceLoader) ServiceLoader.load(ObjectAppender.class);
      Iterator<ObjectAppender<?>> iterator = load.iterator();
      while (iterator.hasNext()) {
        ObjectAppender<?> appender = iterator.next();
        Class<?> appenderType = getAppenderType(appender);
        if (!except.test(appenderType)) {
          if (!register((Class) appenderType, (ObjectAppender) appender)) {
            LOG.warn("Attempting to register duplicate appender({}) for {} ", appender, appenderType);
          }
        }
      }
    }
    for (ObjectAppender<?> appender : appenders) {
      if (!register(getAppenderType(appender), (ObjectAppender) appender)) {
        throw new IllegalArgumentException("Cannot register appender " + appender);
      }
    }
    appenderMap.putIfNotPresent(Object.class, ObjectAppender.TOSTRING_APPENDER);
  }

  public static Class<?> getAppenderType(final ObjectAppender<?> appender) {
    Type[] genericInterfaces = appender.getClass().getGenericInterfaces();
    Class<?> appenderType = null;
    for (Type type : genericInterfaces) {
      if (type instanceof ParameterizedType) {
        ParameterizedType pType = (ParameterizedType) type;
        if (pType.getRawType() == ObjectAppender.class) {
          appenderType = (Class) pType.getActualTypeArguments()[0];
          break;
        }
      }
    }
    if (appenderType == null) {
      throw new IllegalArgumentException("Improperly declared Appender " + appender);
    }
    return appenderType;
  }

  @SuppressWarnings("unchecked")
  public <T> int register(final Class<T> type, final ObjectAppender<? super T>... appenders) {
    int i = 0;
    for (ObjectAppender<? super T> appender : appenders) {
      if (register(type, appender)) {
        i++;
      }
    }
    return i;
  }

  public <T> void replace(final Class<T> type,
          final Function<ObjectAppender<? super T>, ObjectAppender<? super T>> replace) {
    appenderMap.replace(type, (Function) replace);
  }

  @SuppressWarnings("unchecked")
  @CheckReturnValue
  private <T> boolean register(final Class<T> type, final ObjectAppender<? super T> appender) {
    return appenderMap.putIfNotPresent(type, appender);
  }

  public boolean unregister(final Class<?> type) {
    return appenderMap.remove(type);
  }

  @Override
  public ObjectAppender get(final Type type) {
    return appenderMap.get(type);
  }

  @Override
  public String toString() {
    return "ConfigurableAppenderSupplier{" + "lookup=" + appenderMap  + '}';
  }

}