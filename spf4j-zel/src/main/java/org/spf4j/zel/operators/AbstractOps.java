
package org.spf4j.zel.operators;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zoly
 */
abstract class AbstractOps<LT> implements Operator<LT, Number, Number> {

        //CHECKSTYLE:OFF
        final Map<Class, Operator<LT, Number, Number>> operations;
        //CHECKSTYLE:ON

        public AbstractOps() {
            operations = new HashMap<>();
        }

        @Override
        public  Number op(final LT a, final Number b) {
            final Operator<LT, Number, Number> op = operations.get(b.getClass());
            if (op == null) {
                throw new UnsupportedOperationException("Not supporting " + this + " for " + a + " and " + b);
            }
            return op.op(a, b);
        }

    }