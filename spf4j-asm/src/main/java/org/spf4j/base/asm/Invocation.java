package org.spf4j.base.asm;

import java.lang.reflect.Method;
import java.util.Arrays;

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




}
