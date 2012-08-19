/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.stackmonitor;

import com.zoltran.base.HtmlUtils;
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

    public Method(StackTraceElement elem) {
        this.declaringClass = elem.getClassName();
        this.methodName = elem.getMethodName();
    }
    
    public Method(Class<?> clasz, String methodName) {
        this.declaringClass = clasz.getName();
        this.methodName = methodName;
    }
    
    public Method(String declaringClass, String methodName) {
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
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Method other = (Method) obj;
        if ((this.declaringClass == null) ? (other.declaringClass != null) : !this.declaringClass.equals(other.declaringClass)) {
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
        hash = 67 * hash + (this.methodName != null ? this.methodName.hashCode() : 0);
        return hash;
    }
    
    
    @Override
    public String toString() {
        return methodName + "@" + declaringClass;
    }
    
    
    public void toWriter(Writer w) throws IOException {
        w.append(methodName).append("@").append(declaringClass);
    }
    
    public void toHtmlWriter(Writer w) throws IOException {
        w.append(HtmlUtils.htmlEscape( methodName)).append(HtmlUtils.htmlEscape("@")).append(HtmlUtils.htmlEscape(declaringClass));
    }
    
    
    
    public static final Method ROOT= new Method(ManagementFactory.getRuntimeMXBean().getName(), "ROOT");
    
}
