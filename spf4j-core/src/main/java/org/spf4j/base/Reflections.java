package org.spf4j.base;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
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

    private static final Method GET_METHOD0;
    private static final Method GET_CONSTRUCTOR0;

    static {
        PRIMITIVE_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_MAP.put(byte.class, Byte.class);
        PRIMITIVE_MAP.put(char.class, Character.class);
        PRIMITIVE_MAP.put(short.class, Short.class);
        PRIMITIVE_MAP.put(int.class, Integer.class);
        PRIMITIVE_MAP.put(long.class, Long.class);
        PRIMITIVE_MAP.put(float.class, Float.class);
        PRIMITIVE_MAP.put(double.class, Double.class);
        GET_METHOD0 = AccessController.doPrivileged(new PrivilegedAction<Method>() {
                @Override
                public Method run() {
                    Method method;
                    try {
                        //getMethod0(String name, Class<?>[] parameterTypes)
                        method = Class.class.getDeclaredMethod("getMethod0", String.class, Class[].class);
                    } catch (SecurityException ex) {
                        throw new RuntimeException(ex);
                    } catch (NoSuchMethodException ex) {
                        LOG.warn("Class.getMethod0 optimization not available");
                        method = null;
                    }
                    if (method != null) {
                        method.setAccessible(true);
                    }
                    return method;
                }
            });
        GET_CONSTRUCTOR0 = AccessController.doPrivileged(new PrivilegedAction<Method>() {
                @Override
                public Method run() {
                    Method method;
                    try {
                        //getConstructor0(Class<?>[] parameterTypes, int which)
                        method = Class.class.getDeclaredMethod("getConstructor0", Class[].class, int.class);
                    } catch (SecurityException ex) {
                        throw new RuntimeException(ex);
                    } catch (NoSuchMethodException ex) {
                        LOG.warn("Class.getConstructor0 optimization not available");
                        method = null;
                    }
                    if (method != null) {
                        method.setAccessible(true);
                    }
                    return method;
                }
            });

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
     * @param c
     * @param methodName
     * @param paramTypes
     * @return
     */
    @Nullable
    public static Method getMethod(final Class<?> c,
            final String methodName,
            final Class<?>... paramTypes) {
        if (GET_METHOD0 == null) {
            try {
                return c.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        } else {
            try {
                return (Method) GET_METHOD0.invoke(c, methodName, paramTypes);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Equivalent to Class.getConstructor, only that it does not throw an exception.
     */

    @Nullable
    public static Constructor<?> getConstructor(final Class<?> c, final Class<?>... paramTypes) {
        if (GET_CONSTRUCTOR0 == null) {
            try {
                return c.getConstructor(paramTypes);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        } else {
            try {
                return (Constructor) GET_METHOD0.invoke(c, paramTypes, Member.PUBLIC);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    /**
     * Method lookup utility that looks up a method declaration that is compatible.
     * (taking in consideration boxed primitives and varargs)
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


    public static Object invoke(final Method m, final Object object, final Object [] parameters)
            throws IllegalAccessException, InvocationTargetException {
        int np = parameters.length;
        if (np > 0) {
            Class<?> [] actTypes = m.getParameterTypes();
            Class<?> lastParamClass =  actTypes[actTypes.length - 1];
            if (Reflections.canAssign(lastParamClass, parameters[np - 1].getClass())) {
                return m.invoke(object, parameters);
            } else if (lastParamClass.isArray()) {
                int lidx = actTypes.length - 1;
                int l = np - lidx;
                Object array = Array.newInstance(lastParamClass.getComponentType(), l);
                for (int k = 0; k < l; k++) {
                    Array.set(array, k, parameters[lidx + k]);
                }
                Object [] newParams  = new Object [actTypes.length];
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

        public MethodDesc(final Class<?> clasz, final String name, final Class<?>[] paramTypes) {
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
                        public Method load(final MethodDesc k) throws Exception {
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
     * @param clasz
     * @return
     */

    @Nullable
    public static URL getJarSourceUrl(final Class<?> clasz) {
        return clasz.getProtectionDomain().getCodeSource().getLocation();
    }

    /**
     * Get the manifest of a jar file.
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

}
