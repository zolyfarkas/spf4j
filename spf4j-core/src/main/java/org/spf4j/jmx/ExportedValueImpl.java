/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.spf4j.jmx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.InvalidAttributeValueException;
import org.spf4j.base.Throwables;

/**
 *
 * @author zoly
 */
class ExportedValueImpl implements ExportedValue<Object> {
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
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EXS_EXCEPTION_SOFTENING_NO_CHECKED")
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
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EXS_EXCEPTION_SOFTENING_HAS_CHECKED")
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
        return "ExportedValueImpl{" + "name=" + name + ", description=" + description + ", getMethod="
                + getMethod + ", setMethod=" + setMethod + ", object=" + object + ", valueClass=" + valueClass + '}';
    }

}
