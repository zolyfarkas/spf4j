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
package org.spf4j.stackmonitor;

import org.spf4j.base.HtmlUtils;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import javax.annotation.concurrent.Immutable;

/**
 * @author zoly
 */
@Immutable
public final class Method {
    private final String declaringClass;
    private final String methodName;

    public Method(final StackTraceElement elem) {
        this.declaringClass = elem.getClassName();
        this.methodName = elem.getMethodName();
    }
    
    public Method(final Class<?> clasz, final String methodName) {
        this.declaringClass = clasz.getName();
        this.methodName = methodName;
    }
    
    public Method(final String declaringClass, final String methodName) {
        this.declaringClass = declaringClass;
        this.methodName = methodName;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Method other = (Method) obj;
        if ((this.declaringClass == null)
                ? (other.declaringClass != null) : !this.declaringClass.equals(other.declaringClass)) {
            return false;
        }
        if ((this.methodName == null) ? (other.methodName != null) : !this.methodName.equals(other.methodName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.declaringClass != null ? this.declaringClass.hashCode() : 0);
        return 67 * hash + (this.methodName != null ? this.methodName.hashCode() : 0);
    }
    
    
    @Override
    public String toString() {
        return methodName + "@" + declaringClass;
    }
    
    
    public void toWriter(final Writer w) throws IOException {
        w.append(methodName).append("@").append(declaringClass);
    }
    
    public void toHtmlWriter(final Writer w) throws IOException {
        w.append(HtmlUtils.htmlEscape(methodName)).append(HtmlUtils.htmlEscape("@")).
                append(HtmlUtils.htmlEscape(declaringClass));
    }
        
    public static final Method ROOT = new Method(ManagementFactory.getRuntimeMXBean().getName(), "ROOT");
    
}
