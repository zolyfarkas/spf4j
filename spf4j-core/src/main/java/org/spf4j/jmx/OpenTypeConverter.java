/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.jmx;

import com.google.common.base.Converter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import org.spf4j.base.Runtime.Version;

/**
 *
 * @author zoly
 */
// FB does not like guava Convertere equals/hashcode.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("HE_INHERITS_EQUALS_USE_HASHCODE")
public final class OpenTypeConverter {

    private OpenTypeConverter() {
    }

    private static class JMX16 {

        private static final Method TO_CONVERTER;
        private static final Method GET_OPEN_TYPE;
        private static final Method TO_OPEN_VALUE;
        private static final Method FROM_OPEN_VALUE;

        static {
            try {
                final Class<?> openConverterClass = Class.forName("com.sun.jmx.mbeanserver.OpenConverter");
                GET_OPEN_TYPE = openConverterClass.getDeclaredMethod("getOpenType");
                TO_CONVERTER = openConverterClass.getMethod("toConverter", java.lang.reflect.Type.class);
                final Class<?> mxbeanLookup = Class.forName("com.sun.jmx.mbeanserver.MXBeanLookup");
                FROM_OPEN_VALUE = openConverterClass.getMethod("fromOpenValue", mxbeanLookup, Object.class);
                TO_OPEN_VALUE = openConverterClass.getDeclaredMethod("toOpenValue", mxbeanLookup, Object.class);
                AccessController.doPrivileged(new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        TO_OPEN_VALUE.setAccessible(true);
                        FROM_OPEN_VALUE.setAccessible(true);
                        TO_CONVERTER.setAccessible(true);
                        GET_OPEN_TYPE.setAccessible(true);
                        return null;
                    }
                });
            } catch (ClassNotFoundException | NoSuchMethodException
                    | SecurityException | IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static class JMX17P {

        private static final Object DEFAULT_MAPPING_FACTORY;
        private static final Method MAPPING_FOR_TYPE;
        private static final Method MAPPING_GET_OPEN_TYPE;
        private static final Method MAPPING_FROM_OPEN_VALUE;
        private static final Method MAPPING_TO_OPEN_VALUE;

        static {
            try {
                Class<?> mxBeanMappingFactoryClass = Class.forName("com.sun.jmx.mbeanserver.MXBeanMappingFactory");
                DEFAULT_MAPPING_FACTORY = mxBeanMappingFactoryClass.getField("DEFAULT").get(null);
                MAPPING_FOR_TYPE = mxBeanMappingFactoryClass.getMethod("mappingForType",
                                java.lang.reflect.Type.class, mxBeanMappingFactoryClass);
                Class<?> mxBeanMappingClass = Class.forName("com.sun.jmx.mbeanserver.MXBeanMapping");
                MAPPING_GET_OPEN_TYPE = mxBeanMappingClass.getMethod("getOpenType");
                MAPPING_FROM_OPEN_VALUE = mxBeanMappingClass.getDeclaredMethod("fromOpenValue", Object.class);
                MAPPING_TO_OPEN_VALUE = mxBeanMappingClass.getDeclaredMethod("toOpenValue", Object.class);
                AccessController.doPrivileged(new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        MAPPING_FOR_TYPE.setAccessible(true);
                        MAPPING_GET_OPEN_TYPE.setAccessible(true);
                        MAPPING_FROM_OPEN_VALUE.setAccessible(true);
                        MAPPING_TO_OPEN_VALUE.setAccessible(true);
                        return null;
                    }
                });
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            } catch (SecurityException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (NoSuchFieldException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static Object getOpenConverter(final Class<?> type) {
        try {
            return JMX16.TO_CONVERTER.invoke(null, type);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Object openConverterFromOpenValue(final Object converter, final Object openTypeVal) {
        try {
            return JMX16.FROM_OPEN_VALUE.invoke(converter, null, openTypeVal);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Object openConverterToOpenValue(final Object converter, final Object val) {
        try {
            return JMX16.TO_OPEN_VALUE.invoke(converter, null, val);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static OpenType getOpenType(final Object converter) {
        try {
            return (OpenType) JMX16.GET_OPEN_TYPE.invoke(converter);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * returns MXBeanMapping
     */
    private static Object getMXBeanMapping(final Class<?> type) {
        try {
            return JMX17P.MAPPING_FOR_TYPE.invoke(JMX17P.DEFAULT_MAPPING_FACTORY, type, JMX17P.DEFAULT_MAPPING_FACTORY);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static OpenType getOpenType(final Class<?> type) {
        if (org.spf4j.base.Runtime.JAVA_PLATFORM == Version.V1_6) {
            Object converter = getOpenConverter(type);
            return getOpenType(converter);
        } else if (org.spf4j.base.Runtime.JAVA_PLATFORM.ordinal() >= Version.V1_7.ordinal()) {
            try {
                return (OpenType) JMX17P.MAPPING_GET_OPEN_TYPE.invoke(getMXBeanMapping(type));
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            throw new RuntimeException("Unsupported JVM runtime" + org.spf4j.base.Runtime.JAVA_PLATFORM);
        }
    }

    /**
     *
     * @param type
     * @return Converter
     * @throws OpenDataException
     */
    public static Converter<Object, Object> getConverter(final Class<?> type) throws OpenDataException {

        if (org.spf4j.base.Runtime.JAVA_PLATFORM == Version.V1_6) {

            if (Enum.class.isAssignableFrom(type)) {
                return new Converter<Object, Object>() {

                    @Override
                    protected Object doForward(final Object a) {
                        return Enum.valueOf((Class) type, (String) a);
                    }

                    @Override
                    protected Object doBackward(final Object b) {
                        return b.toString();
                    }
                };
            } else {
                final Object conv = getOpenConverter(type);

                return new Converter<Object, Object>() {

                    @Override
                    protected Object doForward(final Object a) {
                        return openConverterFromOpenValue(conv, a);
                    }

                    @Override
                    protected Object doBackward(final Object b) {
                        return openConverterToOpenValue(conv, b);
                    }
                };
            }
        } else if (org.spf4j.base.Runtime.JAVA_PLATFORM.ordinal() >= Version.V1_7.ordinal()) {
            final Object mapping = getMXBeanMapping(type);
            return new Converter<Object, Object>() {

                @Override
                protected Object doForward(final Object a) {
                    try {
                        return JMX17P.MAPPING_FROM_OPEN_VALUE.invoke(mapping, a);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    } catch (InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                protected Object doBackward(final Object b) {
                    try {
                        return JMX17P.MAPPING_TO_OPEN_VALUE.invoke(mapping, b);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    } catch (InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        } else {
            throw new RuntimeException("Unsupported JVM runtime" + org.spf4j.base.Runtime.JAVA_PLATFORM);
        }

    }

}
