
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.InvalidAttributeValueException;


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
    private final Converter<Object, Object> converter;

    ExportedValueImpl(@Nonnull final String name, @Nullable final String description,
            @Nullable final Method getMethod, @Nullable final Method setMethod,
            @Nullable final Object object, @Nonnull final Class<?> valueClass) {
        this.name = name;
        this.description = description;
        this.getMethod = getMethod;
        this.setMethod = setMethod;
        this.object = object;
        this.valueClass = valueClass;
        if (valueClass == Boolean.class
                || valueClass == String.class
                || valueClass.isPrimitive()
                || Number.class.isAssignableFrom(valueClass)) {
            this.converter = null;
        } else {
            this.converter = OpenTypeConverter.getConverter(valueClass);
        }
    }

    public ExportedValueImpl withSetter(@Nonnull final Method psetMethod) {
        if (setMethod != null) {
            throw new IllegalArgumentException("duplicate value registration attemted " + setMethod
                    + ", " + psetMethod);
        }
        return new ExportedValueImpl(name, description, getMethod, psetMethod, object, valueClass);
    }

    public ExportedValueImpl withGetter(@Nonnull final Method pgetMethod, @Nonnull final String pdescription) {
        if (getMethod != null) {
            throw new IllegalArgumentException("duplicate value registration attemted " + getMethod
                    + ", " + pgetMethod);
        }
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
            if (converter != null) {
                return converter.reverse().convert(getMethod.invoke(object));
            } else {
                return getMethod.invoke(object);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
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
            if (converter != null) {
                setMethod.invoke(object, converter.convert(value));
            } else {
                setMethod.invoke(object, value);
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
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
