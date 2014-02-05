
package org.spf4j.zel.operators;

import java.math.MathContext;

/**
 *
 * @author zoly
 */
public interface Operator<A, B> {
    
    enum Enum { Add, Sub, Div, Mul, Mod, Pow }
    
    Object op(A a, B b);
    
    ThreadLocal<MathContext> MATH_CONTEXT = new ThreadLocal<MathContext>() {

        @Override
        protected MathContext initialValue() {
            return MathContext.DECIMAL128;
        }
            
    };
        
}
