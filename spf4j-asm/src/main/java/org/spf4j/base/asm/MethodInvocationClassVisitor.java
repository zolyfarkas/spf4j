package org.spf4j.base.asm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
    return new MethodVisitorImpl(Opcodes.ASM5, methodDesc, methodName);
  }

  @Override
  public String toString() {
    return "MethodInvocationClassVisitor{" + "addCaleesTo=" + addCaleesTo + ", methodStrings=" + methodStrings
            + ", className=" + className + ", source=" + source + '}';
  }

  private class MethodVisitorImpl extends MethodVisitor {

    private final String methodDesc;
    private final String methodName;
    private int lineNumber;
    private final Stack<Object> stack;

    MethodVisitorImpl(final int api, final String methodDesc, final String methodName) {
      super(api);
      this.methodDesc = methodDesc;
      this.methodName = methodName;
      this.stack = new Stack<>();
      Type[] argumentTypes = Type.getArgumentTypes(methodDesc);
      for (int i = 0; i < argumentTypes.length; i++) {
        stack.add(new UnknownValue(-1));
      }
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
            final boolean itf) {
      final Method invokedMethod = methodStrings.get(owner + '/' + name + desc);
      Type returnType = Type.getReturnType(desc);
      Type[] parameterTypes = Type.getArgumentTypes(desc);
      if (invokedMethod != null) {
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = parameterTypes.length - 1; i >= 0; i--) {
          if (!stack.isEmpty()) {
            parameters[i] = stack.pop();
          } else {
            throw new RuntimeException("Not enough params in stack for invocation of " + desc + " at "
                    + className + '.' + methodName + '.' + lineNumber);
          }
        }
        addCaleesTo.add(new Invocation(className, methodName, methodDesc, parameters,
                source, lineNumber, invokedMethod));
        if (opcode != Opcodes.INVOKESTATIC) {
          if (!stack.isEmpty()) {
            stack.pop();
          }
        }
      } else {
        int length = parameterTypes.length;
        if (opcode != Opcodes.INVOKESTATIC) {
          length++;
        }
        for (int i = 0; i < length; i++) {
          if (!stack.isEmpty()) {
            stack.pop();
          }
        }
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
    public void visitTypeInsn(final int opcode, final String type) {
      stack.push(new UnknownValue(opcode));
    }

    @Override
    @SuppressFBWarnings({"PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", "CC_CYCLOMATIC_COMPLEXITY"})
    public void visitInsn(final int opcode) {
      Object pop1 = null;
      Object pop2 = null;
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
        case Opcodes.DUP:
        case Opcodes.DUP2: // we do not differentiate betwen types with mutiple words.
          Object pop = stack.peek();
          stack.push(pop);
          break;
        case Opcodes.DUP2_X1:
        case Opcodes.DUP_X1:
          Object a1 = stack.peek();
          Object b1 = stack.peek();
          stack.push(a1);
          stack.push(b1);
          stack.push(a1);
          break;
        case Opcodes.DUP2_X2:
        case Opcodes.DUP_X2:
          Object a2 = stack.peek();
          Object b2 = stack.peek();
          Object c2 = stack.peek();
          stack.push(a2);
          stack.push(c2);
          stack.push(b2);
          stack.push(a2);
          break;
        case Opcodes.IDIV:
          pop1 = stack.pop();
          pop2 = stack.pop();
          try {
            stack.push((int) pop2 / (int) pop1);
          } catch (RuntimeException ex) {
            stack.push(new UnknownValue(opcode));
          }
          break;
        case Opcodes.IMUL:
          pop1 = stack.pop();
          pop2 = stack.pop();
          try {
            stack.push((int) pop2 * (int) pop1);
          } catch (RuntimeException ex) {
            stack.push(new UnknownValue(opcode));
          }
          break;
         case Opcodes.IADD:
          pop1 = stack.pop();
          pop2 = stack.pop();
          try {
            stack.push((int) pop2 + (int) pop1);
          } catch (RuntimeException ex) {
            stack.push(new UnknownValue(opcode));
          }
          break;
         case Opcodes.ISUB:
          pop1 = stack.pop();
          pop2 = stack.pop();
          try {
            stack.push((int) pop2 - (int) pop1);
          } catch (RuntimeException ex) {
            stack.push(new UnknownValue(opcode));
          }
          break;
        case Opcodes.LDIV:
          pop1 = stack.pop();
          pop2 = stack.pop();
          try {
            stack.push((long) pop2 / (long) pop1);
          } catch (RuntimeException ex) {
            stack.push(new UnknownValue(opcode));
          }
          break;
        case Opcodes.LMUL:
          pop1 = stack.pop();
          pop2 = stack.pop();
          try {
            stack.push((long) pop2 * (long) pop1);
          } catch (RuntimeException ex) {
            stack.push(new UnknownValue(opcode));
          }
          break;
         case Opcodes.LADD:
          pop1 = stack.pop();
          pop2 = stack.pop();
          try {
            stack.push((long) pop2 + (long) pop1);
          } catch (RuntimeException ex) {
            stack.push(new UnknownValue(opcode));
          }
          break;
         case Opcodes.LSUB:
          pop1 = stack.pop();
          pop2 = stack.pop();
          try {
            stack.push((long) pop2 - (long) pop1);
          } catch (RuntimeException ex) {
            stack.push(new UnknownValue(opcode));
          }
          break;
        default:
          stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
      }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
      switch (opcode) {
        case Opcodes.GETSTATIC:
          Class<?> clasz;
          try {
            clasz = Class.forName(owner.replace('/', '.'));
          } catch (ClassNotFoundException ex) {
            stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
            break;
          }
          Field declaredField;
          try {
            declaredField = clasz.getDeclaredField(name);
          } catch (NoSuchFieldException | SecurityException ex) {
            stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
            break;
          }
          if ((declaredField.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
              @Override
              public Void run() {
                declaredField.setAccessible(true);
                return null;
              }
            });
            Object value;
            try {
              value = declaredField.get(null);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
              stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
              break;
            }
            stack.push(value);
          } else {
            stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
          }
          break;
        case Opcodes.GETFIELD:
          stack.push(new UnknownValue(opcode)); // assume something will end up on the stack
          break;
        case Opcodes.PUTSTATIC:
        case Opcodes.PUTFIELD:
          if (!stack.isEmpty()) {
            stack.pop();
          }
          break;
        default:
          throw new IllegalStateException(" Illegal opcode = " + opcode + " in context");
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
        if (!stack.isEmpty()) {
          stack.pop();
        }
//        else {
//          throw new RuntimeException("Not enough params in stack for instr " + opcode + " at "
//                  + className + '.' + methodName + '.' + lineNumber);
//        }
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
      // I am not sure this classic method handling is correct
      Type[] argumentTypes = Type.getArgumentTypes(desc);
      for (int i = 0; i < argumentTypes.length; i++) {
        stack.pop();
      }
      Type returnType = Type.getReturnType(desc);
      if (returnType != Type.VOID_TYPE) {
        stack.push(new UnknownValue(Opcodes.INVOKEDYNAMIC)); // return value
      }
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
      this.lineNumber = line;
    }
  }

}
