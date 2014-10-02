
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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.management.MBeanParameterInfo;
import org.spf4j.base.Reflections;

/**
 *
 * @author zoly
 */
final class ExportedOperationImpl implements ExportedOperation {
    
    private final String name;
    
    private final String description;
    
    private final Method method;
    
    private final Object object;
    
    private final  MBeanParameterInfo[] paramInfos;

    public ExportedOperationImpl(final String name, final String description,
            final Method method, final Object object) {
        this.name = name;
        this.description = description;
        this.method = method;
        this.object = object;
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        paramInfos = new MBeanParameterInfo[parameterTypes.length];
        for (int i = 0; i < paramInfos.length; i++) {
            Annotation [] annotations = parameterAnnotations[i];
            String pname = "";
            String pdesc = "";
            for (Annotation annot : annotations) {
                if (annot.getClass() == JmxExport.class) {
                    pname = (String) Reflections.getAnnotationAttribute(annot, "name");
                    pdesc = (String) Reflections.getAnnotationAttribute(annot, "description");
                }
            }
            if ("".equals(pname)) {
                pname = "param_" + i;
            }
            paramInfos[i] = new MBeanParameterInfo(pname, parameterTypes[i].getName(), pdesc);
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
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    public Object invoke(final Object[] parameters) {
        try {
            return method.invoke(object, parameters);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public MBeanParameterInfo[] getParameterInfos() {
        return paramInfos;
    }

    @Override
    public Class<?> getReturnType() {
        return method.getReturnType();
    }
    
}
