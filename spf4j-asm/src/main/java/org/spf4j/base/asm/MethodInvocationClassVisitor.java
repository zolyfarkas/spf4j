package org.spf4j.base.asm;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * The purpose of this visitor is to find all incovation of certain method. Also to try to get the invocation parameters
 * in case they are know at compile time. Since not all stack operations are simulated yet, the invocation parameters
 * might not be accurate.
 *
 * @author zoly
 */
class MethodInvocationClassVisitor extends ClassVisitor {

  private final Collection<Invocation> addCaleesTo;
  private final Map<String, Method> methodStrings;
  private String className;
  private String source;

  MethodInvocationClassVisitor(final Collection addCaleesTo,
          final Set<Method> methods) {
    super(Opcodes.ASM5);
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
    return new MethodVisitor(Opcodes.ASM5) {

      private int lineNumber;

      private final Stack<Object> stack = new Stack<>();

      {
        Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
        stack.addAll(Arrays.asList(argumentTypes));
      }

      @Override
      public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
              final boolean itf) {
        final Method invokedMethod = methodStrings.get(owner + '/' + name + desc);
        boolean isCallingtarget = invokedMethod != null;
        Type[] parameterTypes = Type.getArgumentTypes(desc);
        Type returnType = Type.getReturnType(desc);
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = parameterTypes.length - 1; i >= 0; i--) {
            parameters[i] = stack.pop();
        }
        if (isCallingtarget) {
          addCaleesTo.add(new Invocation(className, methodName, methodDesc, parameters,
                  source, lineNumber, invokedMethod));
        }
        if (returnType != Type.VOID_TYPE) {
          stack.push(new UnknownValue(opcode)); // return value
        }
      }

      @Override
      public void visitLdcInsn(final Object o) {
        stack.push(o); // push the constant to the stack.
      }

      @Override
      public void visitInsn(final int opcode) {
        switch (opcode) {
          case Opcodes.ICONST_0:
            stack.push(0);
            break;
          case Opcodes.ICONST_1:
            stack.push(1);
            break;
          case Opcodes.ICONST_2:
            stack.push(2);
            break;
          case Opcodes.ICONST_3:
            stack.push(3);
            break;
          case Opcodes.ICONST_4:
            stack.push(4);
            break;
          case Opcodes.ICONST_5:
            stack.push(5);
            break;
          case Opcodes.ICONST_M1:
            stack.push(-1);
            break;
          case Opcodes.LCONST_0:
            stack.push(0L);
            break;
          case Opcodes.LCONST_1:
            stack.push(1L);
            break;
          case Opcodes.FCONST_0:
            stack.push(0f);
            break;
          case Opcodes.FCONST_1:
            stack.push(1f);
            break;
          case Opcodes.FCONST_2:
            stack.push(2f);
            break;
          case Opcodes.DCONST_0:
            stack.push(0d);
            break;
          case Opcodes.DCONST_1:
            stack.push(1d);
            break;
          case Opcodes.ACONST_NULL:
            stack.push(null);
            break;
          default:
            stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
        }
      }

      @Override
      public void visitIntInsn(final int opcode, final int operand) {
        switch (opcode) {
          case Opcodes.BIPUSH:
          case Opcodes.SIPUSH:
            stack.push(operand);
            break;
          default:
            stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
        }
      }

      @Override
      public void visitVarInsn(final int opcode, final int var) {
        if (opcode >= 21 && opcode <= 45) { // *LOAD* from local var instructions
          stack.push(new UnknownValue(opcode));
        } else if (opcode >= 54 && opcode <= 78) { // *STORE* to local var instructions
          stack.pop();
        } else {
          stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
        }
      }

      @Override
      public void visitMultiANewArrayInsn(final String desc, final int dims) {
        stack.push(new UnknownValue(Opcodes.MULTIANEWARRAY)); // assume something will end up on the stack
      }

      @Override
      public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm,
              final Object... bsmArgs) {
        Type returnType = Type.getReturnType(desc);
        if (returnType != Type.VOID_TYPE) {
          stack.push(new UnknownValue(Opcodes.INVOKEDYNAMIC)); // return value
        }
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
