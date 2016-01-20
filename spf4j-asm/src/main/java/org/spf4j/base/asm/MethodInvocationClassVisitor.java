
package org.spf4j.base.asm;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * The purpose of this visitor is to find all incovation of certain method.
 * Also to try to get the invocation parameters in case they are know at compile time.
 * Since not all stack operations are simulated yet, the invocation parameters might not be accurate.
 *
 * @author zoly
 */
class MethodInvocationClassVisitor extends EmptyVisitor {

    private final Collection<Invocation> addCaleesTo;
    private final Map<String, Method> methodStrings;
    private String className;
    private String source;

    public MethodInvocationClassVisitor(final Collection addCaleesTo,
            final Set<Method> methods) {
        this.addCaleesTo = addCaleesTo;
        this.methodStrings = new HashMap<>(methods.size());
        for (Method m : methods) {
            this.methodStrings.put(TypeUtils.toString(m), m);
        }
    }

    @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName, final String[] interfaces) {
        className = name;
    }

    @Override
    public void visitSource(final String psource, final String debug) {
        this.source = psource;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String methodName,
            final String methodDesc, final String signature,
            final String[] exceptions) {
        return new EmptyVisitor() {

            private int lineNumber;

            private final Deque<Object> stack = new ArrayDeque<>();

            {
                Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
                stack.addAll(Arrays.asList(argumentTypes));
            }

            @Override
            public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
                final Method invokedMethod = methodStrings.get(owner + '/' + name + desc);
                boolean isCallingtarget = invokedMethod != null;
                Type[] parameterTypes = Type.getArgumentTypes(desc);
                Object[] parameters = new Object[parameterTypes.length];
                for (int i = parameterTypes.length - 1; i >= 0; i--) {
                    if (stack.isEmpty()) {
                        parameters[i] = "UNKNOWN";
                    } else {
                        parameters[i] = stack.pop();
                    }
                }
                if (isCallingtarget) {
                    addCaleesTo.add(new Invocation(className, methodName, methodDesc, parameters,
                            source, lineNumber, invokedMethod));
                }
            }

            @Override
            public void visitLdcInsn(final Object o) {
                stack.push(o);
            }

            @Override
            public void visitLineNumber(final int line, final Label start) {
                this.lineNumber = line;
            }

        };
    }

    @Override
    public String toString() {
        return "MethodInvocationClassVisitor{" + "addCaleesTo=" + addCaleesTo + ", methodStrings=" + methodStrings
                + ", className=" + className + ", source=" + source + '}';
    }



}
