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
package org.spf4j.base;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.concurrent.UnboundedLoadingCache;

@ParametersAreNonnullByDefault
public final class Reflections {

  private static final BiMap<Class<?>, Class<?>> PRIMITIVE_MAP = HashBiMap.create(8);

  private static final MethodHandle PARAMETER_TYPES_METHOD_FIELD_GET;
  private static final MethodHandle PARAMETER_TYPES_CONSTR_FIELD_GET;
  private static final MethodHandle FIND_CLASS;

  static {
    initPrimitiveMap();
    Field mPtField = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
      Field f;
      try {
        f = Method.class.getDeclaredField("parameterTypes");
        f.setAccessible(true);
      } catch (NoSuchFieldException | SecurityException ex) {
        Logger.getLogger(Reflections.class.getName()).log(Level.INFO,
                "Para type stealing from Method not supported", ex);
        f = null;
      }
      return f;
    });
    Field cPtField = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
      Field f;
      try {
        f = Constructor.class.getDeclaredField("parameterTypes");
        f.setAccessible(true);
      } catch (NoSuchFieldException | SecurityException ex) {
        Logger.getLogger(Reflections.class.getName()).log(Level.INFO,
                "Para type stealing from Constructor not supported", ex);
        f = null;
      }
      return f;
    });

    MethodHandles.Lookup lookup = MethodHandles.lookup();
    if (mPtField != null) {
      try {
        PARAMETER_TYPES_METHOD_FIELD_GET = lookup.unreflectGetter(mPtField);
      } catch (IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    } else {
      PARAMETER_TYPES_METHOD_FIELD_GET = null;
    }
    if (cPtField != null) {
      try {
        PARAMETER_TYPES_CONSTR_FIELD_GET = lookup.unreflectGetter(cPtField);
      } catch (IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    } else {
      PARAMETER_TYPES_CONSTR_FIELD_GET = null;
    }

    final Method fc = AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
      Method m;
      try {
        m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
        m.setAccessible(true);
      } catch (NoSuchMethodException | SecurityException ex) {
        throw new ExceptionInInitializerError(ex);
      }
      return m;
    });
    try {
      FIND_CLASS = lookup.unreflect(fc);
    } catch (IllegalAccessException ex) {
      throw new ExceptionInInitializerError(ex);
    }

  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  private static void initPrimitiveMap() {
    PRIMITIVE_MAP.put(boolean.class, Boolean.class);
    PRIMITIVE_MAP.put(byte.class, Byte.class);
    PRIMITIVE_MAP.put(char.class, Character.class);
    PRIMITIVE_MAP.put(short.class, Short.class);
    PRIMITIVE_MAP.put(int.class, Integer.class);
    PRIMITIVE_MAP.put(long.class, Long.class);
    PRIMITIVE_MAP.put(float.class, Float.class);
    PRIMITIVE_MAP.put(double.class, Double.class);
  }


  private Reflections() { }



  /**
   * Optimized alternative of Method.getParameterTypes that steals the parameterTypeArry from method
   * instead of copying it.
   *
   * Here is the benchmark comparison for a 5 arg method:
   *
   * Benchmark                                       Mode  Cnt           Score          Error  Units
   * ReflectionsBenchmark.normalGetTypes            thrpt   10   227015245.933 ±  2725143.765  ops/s
   * ReflectionsBenchmark.optimizedGetTypes         thrpt   10  1159407471.306 ± 21204385.301  ops/s
   *
   * @param m
   * @return
   */
  public static Class<?>[] getParameterTypes(final Method m) {
    if (PARAMETER_TYPES_METHOD_FIELD_GET != null) {
      try {
        return (Class<?>[]) PARAMETER_TYPES_METHOD_FIELD_GET.invokeExact(m);
      } catch (Error | RuntimeException ex) {
        throw ex;
      } catch (Throwable ex) {
        throw new UncheckedExecutionException(ex);
      }
    } else {
      return m.getParameterTypes();
    }
  }


  public static Class<?>[] getParameterTypes(final Constructor<?> m) {
    if (PARAMETER_TYPES_CONSTR_FIELD_GET != null) {
      try {
        return (Class<?>[]) PARAMETER_TYPES_CONSTR_FIELD_GET.invokeExact(m);
      } catch (Error | RuntimeException ex) {
        throw ex;
      } catch (Throwable ex) {
        throw new UncheckedExecutionException(ex);
      }
    } else {
      return m.getParameterTypes();
    }
  }

  public static Class<?> primitiveToWrapper(final Class<?> clasz) {
    if (clasz.isPrimitive()) {
      return PRIMITIVE_MAP.get(clasz);
    } else {
      return clasz;
    }
  }

  @Nonnull
  public static Type primitiveToWrapper(final Type type) {
    if (type instanceof Class && ((Class) type).isPrimitive()) {
      return PRIMITIVE_MAP.get((Class) type);
    } else {
      return type;
    }
  }

  public static Class<?> wrapperToPrimitive(final Class<?> clasz) {
    if (clasz.isPrimitive()) {
      return clasz;
    } else {
      return PRIMITIVE_MAP.inverse().get(clasz);
    }
  }

  public static boolean isWrappableOrWrapper(final Class clasz) {
    return (PRIMITIVE_MAP.containsKey(clasz) || PRIMITIVE_MAP.containsValue(clasz));
  }


  public static Object getAnnotationAttribute(@Nonnull final Annotation annot, @Nonnull final String attributeName) {
    for (Method method : annot.annotationType().getDeclaredMethods()) {
      if (method.getName().equals(attributeName)) {
        try {
          return method.invoke(annot);
        } catch (IllegalAccessException | InvocationTargetException ex) {
          throw new UncheckedExecutionException(ex);
        }
      }
    }
    throw new IllegalArgumentException(attributeName + " attribute is not present on annotation " + annot);
  }

  /**
   * Equivalent to Class.getDeclaredMethod which returns a null instead of throwing an exception.
   * returned method is also made accessible.
   *
   * @param c
   * @param methodName
   * @param paramTypes
   * @return
   */
  @Nullable
  @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "comparing interned strings")
  public static Method getMethod(final Class<?> c,
          final String methodName,
          final Class<?>... paramTypes) {

    String internedName = methodName.intern();
    for (Method m : c.getDeclaredMethods()) {
      if (m.getName() == internedName
              && Arrays.equals(paramTypes, getParameterTypes(m))) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
          @Override
          public Void run() {
             m.setAccessible(true);
             return null;
          }
        });
        return m;
      }
    }
    return null;
  }

  /**
   * Equivalent to Class.getDeclaredConstructor, only that it does not throw an exception if no constructor,
   * it returns null instead, also makes all constructors accessible..
   */
  @Nullable
  public static Constructor<?> getConstructor(final Class<?> c, final Class<?>... paramTypes) {
    for (Constructor cons : c.getDeclaredConstructors()) {
      if (Arrays.equals(getParameterTypes(cons), paramTypes)) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
          cons.setAccessible(true);
          return null;
        });
        return cons;
      }
    }
    return null;
  }

  /**
   * Method lookup utility that looks up a method declaration that is compatible. (taking in consideration boxed
   * primitives and varargs)
   *
   * @param c
   * @param methodName
   * @param paramTypes
   * @return
   */
  @Nullable
  public static Method getCompatibleMethod(final Class<?> c,
          final String methodName,
          final Class<?>... paramTypes) {
    Method[] methods = c.getMethods();
    for (Method m : methods) {

      if (!methodName.equals(m.getName())) {
        continue;
      }

      Class<?>[] actualTypes = getParameterTypes(m);
      if (actualTypes.length > paramTypes.length) {
        continue;
      }

      boolean found = true;
      int last = actualTypes.length - 1;
      for (int j = 0; j < actualTypes.length; j++) {
        final Class<?> actType = actualTypes[j];
        final Class<?> paramType = paramTypes[j];
        found = canAssign(actType, paramType);
        if (!found && j == last) {
          if (actType.isArray()) {
            boolean matchvararg = true;
            Class<?> varargType = actType.getComponentType();
            for (int k = j; k < paramTypes.length; k++) {
              if (!canAssign(varargType, paramTypes[k])) {
                matchvararg = false;
                break;
              }
            }
            found = matchvararg;
          }
        } else if (j == last && actualTypes.length < paramTypes.length) {
          found = false;
        }
        if (!found) {
          break;
        }
      }

      if (found) {
        return m;
      }
    }
    return null;
  }

  @Nullable
  public static MethodHandle getCompatibleMethodHandle(final Class<?> c,
          final String methodName,
          final Class<?>... paramTypes) {
    Method compatibleMethod = getCompatibleMethod(c, methodName, paramTypes);
    if (compatibleMethod == null) {
      return null;
    } else {
      try {
        return MethodHandles.lookup().unreflect(compatibleMethod);
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }
  }


  public static Object invoke(final Method m, final Object object, final Object[] parameters)
          throws IllegalAccessException, InvocationTargetException {
    int np = parameters.length;
    if (np > 0) {
      Class<?>[] actTypes = getParameterTypes(m);
      Class<?> lastParamClass = actTypes[actTypes.length - 1];
      if (Reflections.canAssign(lastParamClass, parameters[np - 1].getClass())) {
        return m.invoke(object, parameters);
      } else if (lastParamClass.isArray()) {
        int lidx = actTypes.length - 1;
        int l = np - lidx;
        Object array = Array.newInstance(lastParamClass.getComponentType(), l);
        for (int k = 0; k < l; k++) {
          Array.set(array, k, parameters[lidx + k]);
        }
        Object[] newParams = new Object[actTypes.length];
        System.arraycopy(parameters, 0, newParams, 0, lidx);
        newParams[lidx] = array;
        return m.invoke(object, newParams);
      } else {
        throw new IllegalStateException();
      }
    } else {
      return m.invoke(object);
    }
  }

  public static boolean canAssign(final Class<?> to, final Class<?> from) {
    boolean found = true;
    if (!to.isAssignableFrom(from)) {
      if (to.isPrimitive()) {
        Class<?> boxed = PRIMITIVE_MAP.get(to);
        found = boxed.equals(from);
      } else if (from.isPrimitive()) {
        found = PRIMITIVE_MAP.get(from).equals(to);
      } else {
        found = false;
      }
    }
    return found;
  }

  static final class MethodDesc {

    @Nonnull
    private final Class<?> clasz;
    @Nonnull
    private final String name;
    @Nonnull
    private final Class<?>[] paramTypes;

    MethodDesc(final Class<?> clasz, final String name, final Class<?>[] paramTypes) {
      this.clasz = clasz;
      this.name = name;
      this.paramTypes = paramTypes;
    }

    public Class<?> getClasz() {
      return clasz;
    }

    public String getName() {
      return name;
    }

    public Class<?>[] getParamTypes() {
      return paramTypes;
    }

    @Override
    public int hashCode() {
      return 47 * this.clasz.hashCode() + this.name.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj instanceof MethodDesc) {
        final MethodDesc other = (MethodDesc) obj;
        if (!this.clasz.equals(other.clasz)) {
          return false;
        }
        if (!this.name.equals(other.name)) {
          return false;
        }
        return (Arrays.equals(this.paramTypes, other.paramTypes));
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return "MethodDesc{" + "clasz=" + clasz + ','
              + " name=" + name + ", paramTypes=" + Arrays.toString(paramTypes) + '}';
    }

  }

  private static final LoadingCache<MethodDesc, Optional<Method>> CACHE_FAST
          = new UnboundedLoadingCache<>(64,
                  new CacheLoader<MethodDesc, Optional<Method>>() {
            @Override
            public Optional<Method> load(final MethodDesc k) {
              final Method m = getCompatibleMethod(k.getClasz(), k.getName(), k.getParamTypes());
              if (m == null) {
                return Optional.empty();
              }
              AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                  m.setAccessible(true);
                  return null; // nothing to return
                }
              });
              return Optional.of(m);
            }
          });

  @Nullable
  public static Method getCompatibleMethodCached(final Class<?> c,
          final String methodName,
          final Class<?>... paramTypes) {
    return CACHE_FAST.getUnchecked(new MethodDesc(c, methodName, paramTypes)).orElse(null);
  }

  private static final LoadingCache<MethodDesc, MethodHandle> CACHE_FAST_MH
          = new UnboundedLoadingCache<>(64,
                  new CacheLoader<MethodDesc, MethodHandle>() {
            @Override
            public MethodHandle load(final MethodDesc k) {
              return getCompatibleMethodHandle(k.getClasz(), k.getName(), k.getParamTypes());
            }
          });

  public static MethodHandle getCompatibleMethodHandleCached(final Class<?> c,
          final String methodName,
          final Class<?>... paramTypes) {
    return CACHE_FAST_MH.getUnchecked(new MethodDesc(c, methodName, paramTypes));
  }


  /**
   * Get the manifest of a jar file.
   *
   * @param jarUrl
   * @return
   * @throws IOException
   */
  @Nullable
  @SuppressFBWarnings({ "NP_LOAD_OF_KNOWN_NULL_VALUE", "URLCONNECTION_SSRF_FD" })
  public static Manifest getManifest(@Nonnull final URL jarUrl) throws IOException {
    try (JarInputStream jis = new JarInputStream(jarUrl.openStream())) {
      return jis.getManifest();
    }
  }

  /**
   * create a proxy instance that will proxy interface methods to static methods on the target class.
   * @param <T>
   * @param clasz
   * @param target
   * @return
   */
  public static <T> T implementStatic(final Class<T> clasz, final Class<?> target) {

    Method[] methods = clasz.getMethods();
    final Map<Method, Method> map = new HashMap<>(methods.length);
    for (Method from : methods) {
      Method to = getCompatibleMethodCached(target, from.getName(), getParameterTypes(from));
      if (to == null || !Modifier.isStatic(to.getModifiers())) {
        throw new IllegalArgumentException("Cannot map from " + clasz + " to " + target);
      }
      map.put(from, to);
    }

    return (T) Proxy.newProxyInstance(clasz.getClassLoader(), new Class[]{clasz},
            (final Object proxy, final Method method, final Object[] args) -> map.get(method).invoke(null, args));
  }

  public static <T> T implement(final Class<T> clasz, final Object target) {

    Method[] methods = clasz.getMethods();
    final Map<Method, Method> map = new HashMap<>(methods.length);
    Class<? extends Object> aClass = target.getClass();
    for (Method from : methods) {
      Method to = getCompatibleMethodCached(aClass, from.getName(), getParameterTypes(from));
      if (to == null) {
        throw new IllegalArgumentException("Cannot map from " + clasz + " to " + target);
      }
      map.put(from, to);
    }


    return (T) Proxy.newProxyInstance(clasz.getClassLoader(), new Class[]{clasz},
            (final Object proxy, final Method method, final Object[] args) ->
                    map.get(method).invoke(target, args));
  }


  @Nullable
  public static Class<?> getLoadedClass(final ClassLoader cl, final String className) {
    try {
      return (Class<?>) FIND_CLASS.invoke(cl, className);
    } catch (RuntimeException | Error ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new UncheckedExecutionException(ex);
    }

  }

}
