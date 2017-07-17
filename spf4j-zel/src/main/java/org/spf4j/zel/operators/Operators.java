/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

  private static final Class<?>[] IMPLS
          = {IntegerOperators.class, DoubleOperators.class, LongOperators.class,
            BigIntegerOperators.class, BigDecimalOperators.class};

  private static final Map<Class<?>, Operator<Object, Object, Object>>[] OPS;

  static {
    final Operator.Enum[] operators = Operator.Enum.values();
    OPS = new Map[operators.length];
    for (int i = 0; i < OPS.length; i++) {
      OPS[i] = new HashMap<>();
    }
    Set<String> ops = new HashSet<>(operators.length);
    for (Operator.Enum en : operators) {
      ops.add(en.toString());
    }
    for (Class<?> impl : IMPLS) {
      Class<?>[] subClasses = impl.getClasses();
      for (Class<?> subClasz : subClasses) {
        String claszName = subClasz.getSimpleName();
        if (ops.contains(claszName)) {
          try {
            Type type = subClasz.getGenericSuperclass();
            Class<?> leftOClasz;
            if (type instanceof ParameterizedType) {
              leftOClasz = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
            } else {
              throw new ExceptionInInitializerError("Operators class improperly implemented " + subClasz);
            }
            Operator<?, ?, ?> op = (Operator<?, ?, ?>) subClasz.newInstance();
            Operator.Enum ope = Operator.Enum.valueOf(claszName);
            OPS[ope.ordinal()].put(leftOClasz, (Operator<Object, Object, Object>) op);
          } catch (InstantiationException | IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
          }
        }
      }
    }
  }

  private Operators() {
  }

  public static Object apply(final Operator.Enum op, final Object a, final Object b) {
    return OPS[op.ordinal()].get(a.getClass()).op(a, b);
  }

}
