
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import org.spf4j.base.Reflections;
import org.spf4j.base.Strings;

public final class Registry {

    private Registry() { }

    private static final MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();

    private static final Map<ObjectName, Object> REGISTERED = new HashMap<>();

    public static synchronized Object registerMBean(final ObjectName objectName, final Object mbean) {
        Object replaced = null;
        if (MBEAN_SERVER.isRegistered(objectName)) {
            try {
                replaced = REGISTERED.remove(objectName);
                MBEAN_SERVER.unregisterMBean(objectName);
            } catch (InstanceNotFoundException | MBeanRegistrationException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            MBEAN_SERVER.registerMBean(mbean, objectName);
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException ex) {
            throw new RuntimeException(ex);
        }
        REGISTERED.put(objectName, mbean);
        return replaced;
    }

    public static synchronized  Object getRegistered(final ObjectName objectName) {
        return REGISTERED.get(objectName);
    }

    public static synchronized  Object getRegistered(final String domain, final String name) {
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
                throw new RuntimeException(ex);
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

    public static synchronized ExportedValuesMBean export(final String packageName, final String mbeanName,
            final Object ... objects) {
        return export(packageName, mbeanName, (Map) null, objects);
    }

    @SuppressFBWarnings("OCP_OVERLY_CONCRETE_PARAMETER")
    public static synchronized ExportedValuesMBean export(final String packageName, final String mbeanName,
            final Properties attributes, final Object ... objects) {
        return export(packageName, mbeanName, (Map) attributes, objects);
    }

    public static synchronized ExportedValuesMBean export(final String packageName, final String mbeanName,
            final Map<String, Object> attributes, final Object ... objects) {

        ObjectName objectName = ExportedValuesMBean.createObjectName(packageName, mbeanName);
        ExportedValuesMBean existing = (ExportedValuesMBean) unregister(objectName);
        Map<String, ExportedValue> exportedAttributes = new HashMap<>();
        if (attributes != null) {
            for (String attrName : attributes.keySet()) {
                exportedAttributes.put(attrName, new MapExportedValue(attributes, null, attrName));
            }
        }
        Map<String, ExportedOperationImpl> exportedOps = new HashMap<>();
        for (Object object : objects) {

            if (object instanceof Class) {
                for (Method method : ((Class<?>) object).getMethods()) {
                    if (method.isSynthetic()) {
                        continue;
                    }
                    if (Modifier.isStatic(method.getModifiers())) {
                        Annotation[] annotations = method.getAnnotations();
                        for (Annotation annot : annotations) {
                            if (annot.annotationType() == JmxExport.class) {
                                exportMethod(method, null, exportedAttributes, exportedOps, annot);
                            }
                        }
                    }
                }
            } else {
                final Class<? extends Object> oClass = object.getClass();
                for (Method method : oClass.getMethods()) {
                    if (method.isSynthetic()) {
                        continue;
                    }
                    Annotation[] annotations = method.getAnnotations();
                    for (Annotation annot : annotations) {
                        if (annot.annotationType() == JmxExport.class) {
                            exportMethod(method, object, exportedAttributes, exportedOps, annot);
                        }
                    }
                }
            }

        }
        if (exportedAttributes.isEmpty() && exportedOps.isEmpty()) {
            return null;
        }
        ExportedValue<?>[] values = new ExportedValue[exportedAttributes.size()];
        int i = 0;
        for (ExportedValue expVal : exportedAttributes.values()) {
            if (expVal instanceof ExportedValueImpl && !((ExportedValueImpl) expVal).isValid()) {
               throw new IllegalArgumentException("If setter is exported, getter must be exported as well " + expVal);
            } else {
               values[i++] = expVal;
            }
        }
        ExportedValuesMBean mbean;
        if (existing == null) {
            mbean = new ExportedValuesMBean(objectName, values,
                        exportedOps.values().toArray(new ExportedOperation[exportedOps.size()]));
        } else {
            mbean = new ExportedValuesMBean(existing, values,
                        exportedOps.values().toArray(new ExportedOperation[exportedOps.size()]));
        }
        Registry.registerMBean(mbean.getObjectName(), mbean);
        return mbean;
    }

    private static void exportMethod(final Method method,
            @Nullable final Object object, final Map<String, ExportedValue> exportedAttributes,
            final Map<String, ExportedOperationImpl> exportedOps, final Annotation annot) {
        method.setAccessible(true); // this is to speed up invocation
        String methodName = method.getName();
        int nrParams = method.getParameterTypes().length;
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
            String nameOverwrite = (String) Reflections.getAnnotationAttribute(annot, "value");
            if (!"".equals(nameOverwrite)) {
                opName = nameOverwrite;
            }
            ExportedOperationImpl existing = exportedOps.put(opName, new ExportedOperationImpl(opName,
                    (String) Reflections.getAnnotationAttribute(annot, "description"), method, object));
            if (existing != null) {
                throw new IllegalArgumentException("exporting operations with same name not supported: " + opName);
            }
        }
    }

    private static void addSetter(final String methodName, final Map<String, ExportedValue> exportedAttributes,
            final Method method, final Object object, final Annotation annot) {
        String customName = (String) Reflections.getAnnotationAttribute(annot, "value");
        String valueName;
        if ("".equals(customName)) {
            valueName = methodName.substring("set".length());
            valueName = Strings.withFirstCharLower(valueName);
        } else {
            valueName = customName;
        }

        ExportedValueImpl existing = (ExportedValueImpl) exportedAttributes.get(valueName);
        if (existing == null) {
            existing = new ExportedValueImpl(valueName, null,
                    null, method, object, method.getParameterTypes()[0]);
        } else {
            if (existing.getValueClass() != method.getParameterTypes()[0]) {
                throw new IllegalArgumentException(
                        "Getter and setter icorrectly defined " + existing + ' ' + method);
            }
            existing = existing.withSetter(method);
        }
        exportedAttributes.put(valueName, existing);
    }

    private static void addGetter(final String pvalueName,
            final Map<String, ExportedValue> exported,
            final Annotation annot, final Method method, final Object object) {
        String customName = (String) Reflections.getAnnotationAttribute(annot, "value");
        String valueName = "".equals(customName) ? pvalueName : customName;
        ExportedValueImpl existing = (ExportedValueImpl) exported.get(valueName);
        if (existing == null) {
            existing = new ExportedValueImpl(
                    valueName, (String) Reflections.getAnnotationAttribute(annot, "description"),
                    method, null, object, method.getReturnType());
        } else {
            if (existing.getValueClass() != method.getReturnType()) {
                throw new IllegalArgumentException(
                        "Getter and setter icorrectly defined " + existing + ' ' + method);
            }
            existing = existing.withGetter(method,
                    (String) Reflections.getAnnotationAttribute(annot, "description"));
        }
        exported.put(valueName, existing);
    }
}
