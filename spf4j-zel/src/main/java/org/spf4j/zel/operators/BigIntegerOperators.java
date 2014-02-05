/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.operators;

import com.google.common.math.LongMath;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.spf4j.zel.operators.Operator.MATH_CONTEXT;

public final class BigIntegerOperators {
    
    private BigIntegerOperators() {
    }

    public static final class Add implements Operator<BigInteger, Number> {

        @Override
        public Object op(final BigInteger a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {              
                    return a.add(BigInteger.valueOf(b.longValue()));
            } else if (claszB.equals(Double.class)) {
                return ((Double) b) + a.doubleValue();
            } else if (claszB.equals(Float.class)) {
                return ((Double) b) + a.doubleValue();
            } else if (claszB.equals(BigInteger.class)) {
                return ((BigInteger) b).add(a);
            } else if (claszB.equals(BigDecimal.class)) {
                return ((BigDecimal) b).add(new BigDecimal(a), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Sub implements Operator<BigInteger, Number> {

        @Override
        public Object op(final BigInteger a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                    return a.subtract(BigInteger.valueOf(b.longValue()));
            } else if (claszB.equals(Double.class)) {
                return a.doubleValue() - ((Double) b);
            } else if (claszB.equals(Float.class)) {
                return a.doubleValue() - ((Float) b);
            } else if (claszB.equals(BigInteger.class)) {
                return a.subtract((BigInteger) b);
            } else if (claszB.equals(BigDecimal.class)) {
                return new BigDecimal(a).subtract((BigDecimal) b, MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mul implements Operator<BigInteger, Number> {

        @Override
        public Object op(final BigInteger a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
           
               return a.multiply(BigInteger.valueOf(b.longValue()));                                      
            } else if (claszB.equals(Double.class)) {
                return a.doubleValue() * ((Double) b);
            } else if (claszB.equals(Float.class)) {
                return a.doubleValue() * ((Float) b);
            } else if (claszB.equals(BigInteger.class)) {
                return a.multiply((BigInteger) b);
            } else if (claszB.equals(BigDecimal.class)) {
                return new BigDecimal(a).multiply((BigDecimal) b, MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Div implements Operator<BigInteger, Number> {

        @Override
        public Object op(final BigInteger a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.divide(BigInteger.valueOf(b.longValue()));
            } else if (claszB.equals(Double.class)) {
                return a.doubleValue() / ((Double) b);
            } else if (claszB.equals(Float.class)) {
                return a.doubleValue() / ((Float) b);
            } else if (claszB.equals(BigInteger.class)) {
                return  a.divide((BigInteger) b).intValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return new BigDecimal(a).divide((BigDecimal) b, MATH_CONTEXT.get()).intValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mod implements Operator<BigInteger, Number> {

        @Override
        public Object op(final BigInteger a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.mod(BigInteger.valueOf(b.longValue()));
            } else if (claszB.equals(Double.class)) {
                return a.mod(BigInteger.valueOf(b.longValue()));
            } else if (claszB.equals(Float.class)) {
                return a.mod(BigInteger.valueOf(b.longValue()));
            } else if (claszB.equals(BigInteger.class)) {
                return a.mod((BigInteger) b).intValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return a.mod(((BigDecimal) b).toBigInteger()).intValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Pow implements Operator<BigInteger, Number> {

        @Override
        public Object op(final BigInteger a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.pow(b.intValue());
            } else if (claszB.equals(Long.class)) {
                if (((Long) b).compareTo((long) Integer.MAX_VALUE) > 0) {
                    return Math.pow(a.doubleValue(), (Long) b);
                } else {
                    return a.pow(b.intValue());
                }
            } else if (claszB.equals(Double.class)) {
                return Math.pow(a.doubleValue(), (Double) b);
            } else if (claszB.equals(Float.class)) {
                return Math.pow(a.doubleValue(), (Double) b);
            } else if (claszB.equals(BigInteger.class)) {
                return a.pow(b.intValue());
            } else if (claszB.equals(BigDecimal.class)) {
                return Math.pow(a.doubleValue(), b.doubleValue());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }



}
