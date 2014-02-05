/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.spf4j.zel.operators;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author zoly
 */
public final class Operators {
   
    private Operators() { }
    
    private static final Class<?> [] IMPLS =
    {IntegerOperators.class, DoubleOperators.class, LongOperators.class,
        BigIntegerOperators.class, BigDecimalOperators.class};
    
    private static final Map<Class<?>, Operator<Object, Object>> [] OPS =
            new Map[Operator.Enum.values().length];
   
    static {
        for (int i = 0; i < OPS.length; i++) {
            OPS[i] = new HashMap<Class<?>, Operator<Object, Object>>();
        }
        Set<String> ops = new HashSet<String>(Operator.Enum.values().length);
        for (Operator.Enum en : Operator.Enum.values()) {
            ops.add(en.toString());
        }
        for (Class<?> impl : IMPLS) {
            Class<?> [] subClasses = impl.getClasses();
            for (Class<?> subClasz : subClasses) {
              String claszName = subClasz.getSimpleName();
              if (ops.contains(claszName)) {
                  try {
                      Type type =  subClasz.getGenericInterfaces()[0];
                      Class<?> leftOClasz;
                      if (type instanceof ParameterizedType) {
                        leftOClasz = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
                      } else {
                          throw new RuntimeException("Operators class improperly implemented");
                      }
                      Operator<?, ?> op = (Operator<?, ?>) subClasz.newInstance();
                      Operator.Enum ope = Operator.Enum.valueOf(claszName);
                      OPS[ope.ordinal()].put(leftOClasz, (Operator<Object, Object>) op);
                  } catch (InstantiationException ex) {
                      throw new RuntimeException(ex);
                  } catch (IllegalAccessException ex) {
                      throw new RuntimeException(ex);
                  }
              }
            }
        }
    }
    
    public static Object apply(final Operator.Enum op, final Object a, final Object b) {
        return OPS[op.ordinal()].get(a.getClass()).op(a, b);
    }
    
}
