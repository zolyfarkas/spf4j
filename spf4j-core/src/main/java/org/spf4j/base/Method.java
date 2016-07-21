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
package org.spf4j.base;

import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.hash.THashMap;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * @author zoly
 */
@Immutable
// using racy single check idiom makes findbugs think the Method obejct is mutable...
@SuppressFBWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class Method implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String declaringClass;
    private final String methodName;
    private int hash;

    public Method(final StackTraceElement elem) {
      this(elem.getClassName(), elem.getMethodName());
    }

    public Method(final Class<?> clasz, final String methodName) {
      this(clasz.getName(), methodName);
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
    public int hashCode() {
        if (hash == 0) {
            int nhash = 3;
            nhash = 47 * nhash + (this.declaringClass != null ? this.declaringClass.hashCode() : 0);
            nhash = 47 * nhash + (this.methodName != null ? this.methodName.hashCode() : 0);
            hash = nhash;
        }
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
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
    public String toString() {
        return methodName + '@' + declaringClass;
    }

    public void toWriter(final Writer w) throws IOException {
        w.append(methodName).append("@").append(declaringClass);
    }

    public void toHtmlWriter(final Writer w) throws IOException {
        Escaper htmlEscaper = HtmlEscapers.htmlEscaper();
        w.append(htmlEscaper.escape(methodName)).append(htmlEscaper.escape("@")).
                append(htmlEscaper.escape(declaringClass));
    }
    public static final Method ROOT = new Method(ManagementFactory.getRuntimeMXBean().getName(), "ROOT");


    private static final Map<String, Map<String, Method>> INSTANCE_REPO = new THashMap<>(1024);


    public static Method getMethod(final StackTraceElement elem) {
        return getMethod(elem.getClassName(), elem.getMethodName());
    }

    /*
     * this function is to allow reuse of Method instances.
     * not thread safe, use with care, see description for suppressed findbugs bug for more detail.
     */

    @SuppressFBWarnings("PMB_POSSIBLE_MEMORY")
    public static Method getMethod(final String className, final String methodName) {
        Map<String, Method> mtom = INSTANCE_REPO.get(className);
        Method result;
        if (mtom == null) {
            mtom = new HashMap<>(4);
            result = new Method(className, methodName);
            mtom.put(methodName, result);
            INSTANCE_REPO.put(className, mtom);
        } else {
            result = mtom.get(methodName);
            if (result == null) {
                result = new Method(className, methodName);
                mtom.put(methodName, result);
            }
        }
        return result;
    }

}
