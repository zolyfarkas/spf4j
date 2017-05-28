package org.spf4j.zel.operators;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zoly
 * @param <L> left operand type (ex: L <operator> Number = Number)
 */
abstract class AbstractOps<L> implements Operator<L, Number, Number> {

  //CHECKSTYLE:OFF
  final Map<Class, Operator<L, Number, Number>> operations;
  //CHECKSTYLE:ON

  AbstractOps() {
    operations = new HashMap<>();
  }

  @Override
  public Number op(final L a, final Number b) {
    final Operator<L, Number, Number> op = operations.get(b.getClass());
    if (op == null) {
      throw new UnsupportedOperationException("Not supporting " + this + " for " + a + " and " + b);
    }
    return op.op(a, b);
  }

}
