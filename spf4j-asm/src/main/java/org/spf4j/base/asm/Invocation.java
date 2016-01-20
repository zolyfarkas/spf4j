package org.spf4j.base.asm;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author zoly
 */
public final class Invocation {

    private final String caleeClassName;
    private final String caleeMethodName;
    private final String caleeMethodDesc;
    private final String caleeSource;
    private final int caleeLine;
    private final Object [] parameters;
    private final Method invokedMethod;

    public Invocation(final String cName, final String mName, final String mDesc,
            final Object [] parameters, final String src, final int ln, final Method invokedMethod) {
        caleeClassName = cName;
        caleeMethodName = mName;
        caleeMethodDesc = mDesc;
        this.parameters = parameters.clone();
        caleeSource = src;
        caleeLine = ln;
        this.invokedMethod = invokedMethod;
    }

    public String getCaleeClassName() {
        return caleeClassName;
    }

    public String getCaleeMethodName() {
        return caleeMethodName;
    }

    public String getCaleeMethodDesc() {
        return caleeMethodDesc;
    }

    public String getCaleeSource() {
        return caleeSource;
    }

    public int getCaleeLine() {
        return caleeLine;
    }

    public Method getInvokedMethod() {
        return invokedMethod;
    }



    public Object[] getParameters() {
        return parameters.clone();
    }

    @Override
    public String toString() {
        return "Invocation{" + "caleeClassName=" + caleeClassName + ", caleeMethodName="
                + caleeMethodName + ", caleeMethodDesc=" + caleeMethodDesc + ", caleeSource="
                + caleeSource + ", caleeLine=" + caleeLine + ", parameters=" + Arrays.toString(parameters)
                + ", invokedMethod=" + invokedMethod + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.caleeClassName);
        return 97 * hash + Objects.hashCode(this.caleeMethodName);
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
        final Invocation other = (Invocation) obj;
        if (this.caleeLine != other.caleeLine) {
            return false;
        }
        if (!Objects.equals(this.caleeClassName, other.caleeClassName)) {
            return false;
        }
        if (!Objects.equals(this.caleeMethodName, other.caleeMethodName)) {
            return false;
        }
        if (!Objects.equals(this.caleeMethodDesc, other.caleeMethodDesc)) {
            return false;
        }
        if (!Objects.equals(this.caleeSource, other.caleeSource)) {
            return false;
        }
        if (!Arrays.deepEquals(this.parameters, other.parameters)) {
            return false;
        }
        return Objects.equals(this.invokedMethod, other.invokedMethod);
    }




}
