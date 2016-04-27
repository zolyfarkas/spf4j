package org.spf4j.base;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.concurrent.UnboundedRacyLoadingCache;

@ParametersAreNonnullByDefault
public final class Reflections {

  private Reflections() {
  }

  private static final Logger LOG = LoggerFactory.getLogger(Reflections.class);

  private static final Map<Class<?>, Class<?>> PRIMITIVE_MAP = new HashMap<>(8);

  static {
    PRIMITIVE_MAP.put(boolean.class, Boolean.class);
    PRIMITIVE_MAP.put(byte.class, Byte.class);
    PRIMITIVE_MAP.put(char.class, Character.class);
    PRIMITIVE_MAP.put(short.class, Short.class);
    PRIMITIVE_MAP.put(int.class, Integer.class);
    PRIMITIVE_MAP.put(long.class, Long.class);
    PRIMITIVE_MAP.put(float.class, Float.class);
    PRIMITIVE_MAP.put(double.class, Double.class);
  }

  public static Class<?> primitiveToWrapper(final Class<?> clasz) {
    if (clasz.isPrimitive()) {
      return PRIMITIVE_MAP.get(clasz);
    } else {
      return clasz;
    }
  }

  public static Object getAnnotationAttribute(@Nonnull final Annotation annot, @Nonnull final String attributeName) {
    for (Method method : annot.annotationType().getDeclaredMethods()) {
      if (method.getName().equals(attributeName)) {
        try {
          return method.invoke(annot);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
          throw new RuntimeException(ex);
        }
      }
    }
    throw new IllegalArgumentException(attributeName + " attribute is not present on annotation " + annot);
  }

  /**
   * Equivalent to Class.getMethod which returns a null instead of throwing an exception.
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
              && Arrays.equals(paramTypes, m.getParameterTypes())) {
        return m;
      }
    }
    return null;
  }

  /**
   * Equivalent to Class.getConstructor, only that it does not throw an exception.
   */
  @Nullable
  public static Constructor<?> getConstructor(final Class<?> c, final Class<?>... paramTypes) {
    for (Constructor cons : c.getDeclaredConstructors()) {
      if (Arrays.equals(cons.getParameterTypes(), paramTypes)) {
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

      Class<?>[] actualTypes = m.getParameterTypes();
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

  public static Object invoke(final Method m, final Object object, final Object[] parameters)
          throws IllegalAccessException, InvocationTargetException {
    int np = parameters.length;
    if (np > 0) {
      Class<?>[] actTypes = m.getParameterTypes();
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

  private static final LoadingCache<MethodDesc, Method> CACHE_FAST
          = new UnboundedRacyLoadingCache<>(64,
                  new CacheLoader<MethodDesc, Method>() {
            @Override
            public Method load(final MethodDesc k) {
              final Method m = getCompatibleMethod(k.getClasz(), k.getName(), k.getParamTypes());
              AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                  m.setAccessible(true);
                  return null; // nothing to return
                }
              });
              return m;
            }
          });

  public static Method getCompatibleMethodCached(final Class<?> c,
          final String methodName,
          final Class<?>... paramTypes) {
    return CACHE_FAST.getUnchecked(new MethodDesc(c, methodName, paramTypes));
  }

  /**
   * Useful to get the jar URL where a particular class is located.
   *
   * @param clasz
   * @return
   */
  @Nullable
  public static URL getJarSourceUrl(final Class<?> clasz) {
    final CodeSource codeSource = clasz.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      return null;
    } else {
      return codeSource.getLocation();
    }
  }

  /**
   * Get the manifest of a jar file.
   *
   * @param jarUrl
   * @return
   * @throws IOException
   */
  @Nullable
  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  public static Manifest getManifest(@Nonnull final URL jarUrl) throws IOException {
    try (JarInputStream jis = new JarInputStream(jarUrl.openStream())) {
      return jis.getManifest();
    }
  }

  public static final class PackageInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final URL url;
    private final String version;

    @ConstructorProperties({"url", "version"})
    public PackageInfo(@Nullable final URL url, @Nullable final String version) {
      this.url = url;
      this.version = version;
    }

    @Nullable
    public URL getUrl() {
      return url;
    }

    @Nullable
    public String getVersion() {
      return version;
    }

    @Override
    @SuppressFBWarnings("DMI_BLOCKING_METHODS_ON_URL")
    public int hashCode() {
      if (url != null) {
        return url.hashCode();
      } else {
        return 0;
      }
    }

    public boolean hasInfo() {
      return url != null || version != null;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final PackageInfo other = (PackageInfo) obj;
      if (!java.util.Objects.equals(this.url, other.url)) {
        return false;
      }
      return java.util.Objects.equals(this.version, other.version);
    }

    @Override
    public String toString() {
      return "PackageInfo{" + "url=" + url + ", version=" + version + '}';
    }

  }

  private static final PackageInfo NONE = new PackageInfo(null, null);

  
  @Nonnull
  public static PackageInfo getPackageInfoDirect(@Nonnull final String className) {
    Class<?> aClass;
    try {
      aClass = Class.forName(className);
    } catch (ClassNotFoundException ex) {
      return NONE;
    }
    return getPackageInfoDirect(aClass);
  }

  @Nonnull
  public static PackageInfo getPackageInfoDirect(@Nonnull final Class<?> aClass) {
    URL jarSourceUrl = Reflections.getJarSourceUrl(aClass);
    final Package aPackage = aClass.getPackage();
    if (aPackage == null) {
      return NONE;
    }
    String version = aPackage.getImplementationVersion();
    return new PackageInfo(jarSourceUrl, version);
  }

  @Nonnull
  public static PackageInfo getPackageInfo(@Nonnull final String className) {
    return CACHE.getUnchecked(className);
  }

  private static final LoadingCache<String, PackageInfo> CACHE = CacheBuilder.newBuilder()
          .weakKeys().weakValues().build(new CacheLoader<String, PackageInfo>() {

            @Override
            public PackageInfo load(final String key) {
              return getPackageInfoDirect(key);
            }
          });
  
}
