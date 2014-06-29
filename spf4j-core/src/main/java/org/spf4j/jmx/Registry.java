package org.spf4j.jmx;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.spf4j.base.Reflections;
import org.spf4j.base.Strings;
import org.spf4j.base.Throwables;

public final class Registry {
    
    private Registry() { }

    private static final MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();

    private static void register(final ObjectName objectName, final DynamicMBean mbean) {
        if (MBEAN_SERVER.isRegistered(objectName)) {
            try {
                MBEAN_SERVER.unregisterMBean(objectName);
            } catch (InstanceNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (MBeanRegistrationException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            MBEAN_SERVER.registerMBean(mbean, objectName);
        } catch (InstanceAlreadyExistsException ex) {
            throw new RuntimeException(ex);
        } catch (MBeanRegistrationException ex) {
            throw new RuntimeException(ex);
        } catch (NotCompliantMBeanException ex) {
            throw new RuntimeException(ex);
        }
    }

    static class ExportedValueImpl implements ExportedValue<Object> {
        private final String name;
        private final String description;
        private final Method getMethod;
        private final Method setMethod;
        private final Object object;
        private final Class<?> valueClass;

        ExportedValueImpl(@Nonnull final String name, @Nullable final String description,
                @Nullable final Method getMethod, @Nullable final Method setMethod,
                @Nullable final Object object, @Nonnull final Class<?> valueClass) {
            this.name = name;
            this.description = description;
            this.getMethod = getMethod;
            this.setMethod = setMethod;
            this.object = object;
            this.valueClass = valueClass;
        }

        public ExportedValueImpl withSetter(@Nonnull final Method psetMethod) {
            return new ExportedValueImpl(name, description, getMethod, psetMethod, object, valueClass);
        }
        
        public ExportedValueImpl withGetter(@Nonnull final Method pgetMethod, @Nonnull final String pdescription) {
            return new ExportedValueImpl(name, pdescription, pgetMethod, setMethod, object, valueClass);
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
        @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
        public Object get() {
            try {
                return getMethod.invoke(object);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_HAS_CHECKED")
        public void set(final Object value) throws InvalidAttributeValueException {
            if (setMethod == null) {
                throw new InvalidAttributeValueException(name + " is a read only attribute ");
            }
            try {
                setMethod.invoke(object, value);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw Throwables.suppress(new InvalidAttributeValueException(name + " has an invalid type "), ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public boolean isWriteable() {
            return setMethod != null;
        }

        @Override
        public Class<? extends Object> getValueClass() {
            return valueClass;
        }
        
        public boolean isValid() {
            return getMethod != null;
        }

        @Override
        public String toString() {
            return "ExportedValueImpl{" + "name=" + name + ", description=" + description
                    + ", getMethod=" + getMethod + ", setMethod=" + setMethod
                    + ", object=" + object + ", valueClass=" + valueClass + '}';
        }
        
        
    }
    
    
    public static void export(final String packageName, final String mbeanName, final Object ... objects) {
        
        Map<String, ExportedValueImpl> exported = new HashMap<String, ExportedValueImpl>();
        boolean prependClass = objects.length > 1;
        for (Object object : objects) {
            for (Method method : object.getClass().getMethods()) {
                method.setAccessible(true); // this is to speed up invocation
                Annotation [] annotations = method.getAnnotations();
                for (Annotation annot : annotations) {
                    if (annot.annotationType() == JmxExport.class) {
                        String methodName = method.getName();
                        if (methodName.startsWith("get")) {
                            String valueName = methodName.substring("get".length());
                            valueName = Strings.withFirstCharLower(valueName);
                            if (prependClass) {
                                valueName = object.getClass().getSimpleName() + "." + valueName;
                            }
                            addGetter(valueName, exported, annot, method, object);
                        } else if (methodName.startsWith("is")) {
                            String valueName = methodName.substring("is".length());
                            valueName = Strings.withFirstCharLower(valueName);
                            if (prependClass) {
                                valueName = object.getClass().getSimpleName() + "." + valueName;
                            }
                            addGetter(valueName, exported, annot, method, object);
                        } else if (methodName.startsWith("set")) {
                            String valueName = methodName.substring("set".length());
                            valueName = Strings.withFirstCharLower(valueName);
                            if (prependClass) {
                                valueName = object.getClass().getSimpleName() + "." + valueName;
                            }
                            ExportedValueImpl existing = exported.get(valueName);
                            if (existing == null) {
                                existing = new ExportedValueImpl(valueName, null,
                                    null, method, object, method.getParameterTypes()[0]);
                            } else {
                                if (existing.getValueClass() != method.getParameterTypes()[0]) {
                                    throw new IllegalArgumentException(
                                            "Getter and setter icorrectly defined " + existing + " " + method);
                                }
                                existing = existing.withSetter(method);
                            }
                            exported.put(valueName, existing);
                        }
                    }
                }
            }
        }
        if (exported.isEmpty()) {
            return;
        }
        ExportedValue<?> [] values = new ExportedValue[exported.size()];
        int i = 0;
        for (ExportedValueImpl expVal : exported.values()) {
            if (expVal.isValid()) {
                values[i++] = expVal;
            } else {
                throw new IllegalArgumentException("If setter is exported, getter must be exported as well " + expVal);
            }
        }
        
        ExportedValuesMBean mbean = new ExportedValuesMBean(packageName, mbeanName, values);
        register(mbean.getObjectName(), mbean);
    }

    private static void addGetter(final String valueName,
            final Map<String, ExportedValueImpl> exported,
            final Annotation annot, final Method method, final Object object) {
        ExportedValueImpl existing = exported.get(valueName);
        if (existing == null) {
            existing = new ExportedValueImpl(
                    valueName, (String) Reflections.getAnnotationAttribute(annot, "description"),
                    method, null, object, method.getReturnType());
        } else {
            if (existing.getValueClass() != method.getReturnType()) {
                throw new IllegalArgumentException(
                        "Getter and setter icorrectly defined " + existing + " " + method);
            }
            existing = existing.withGetter(method,
                    (String) Reflections.getAnnotationAttribute(annot, "description"));
        }
        exported.put(valueName, existing);
    }
}
