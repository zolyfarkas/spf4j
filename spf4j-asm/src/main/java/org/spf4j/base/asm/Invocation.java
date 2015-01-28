package org.spf4j.base.asm;

import java.util.Arrays;

/**
 *
 * @author zoly
 */
public final class Invocation {

    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final String source;
    private final int line;
    private final Object [] parameters;

    public Invocation(final String cName, final String mName, final String mDesc,
            final Object [] parameters, final String src, final int ln) {
        className = cName;
        methodName = mName;
        methodDesc = mDesc;
        this.parameters = parameters.clone();
        source = src;
        line = ln;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public String getSource() {
        return source;
    }

    public int getLine() {
        return line;
    }

    public Object[] getParameters() {
        return parameters.clone();
    }

    @Override
    public String toString() {
        return "Callee{" + "className=" + className + ", methodName=" + methodName
                + ", methodDesc=" + methodDesc + ", source=" + source + ", line=" + line
                + ", parameters=" + Arrays.toString(parameters) + '}';
    }
    

}
