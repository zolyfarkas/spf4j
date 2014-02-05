/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.operators;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class DoubleOperators {
    
    private DoubleOperators() {
    }

    public static final class Add implements Operator<Double, Number> {

        @Override
        public Object op(final Double a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return  a + b.intValue();
            } else if (claszB.equals(Long.class)) {
                return a + b.longValue();
            } else if (claszB.equals(Double.class)) {
                return a + b.doubleValue();
            }  else if (claszB.equals(BigInteger.class)) {
                return a + b.doubleValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return a + b.doubleValue();
            } else if (claszB.equals(Float.class)) {
                return a + b.floatValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Sub implements Operator<Double, Number> {

        @Override
        public Object op(final Double a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a - b.intValue();
            } else if (claszB.equals(Long.class)) {
                return a - b.longValue();
            } else if (claszB.equals(Double.class)) {
                return a - b.doubleValue();
            } else if (claszB.equals(BigInteger.class)) {
                return a - b.doubleValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return a - b.doubleValue();
            } else if (claszB.equals(Float.class)) {
                return a - b.floatValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mul implements Operator<Double, Number> {

        @Override
        public Object op(final Double a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a * b.intValue();
            } else if (claszB.equals(Long.class)) {
                return a * b.longValue();
            } else if (claszB.equals(Double.class)) {
                return a * ((Double) b);
            } else if (claszB.equals(BigInteger.class)) {
                return a * b.doubleValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return a * b.doubleValue();
            } else if (claszB.equals(Double.class)) {
                return a * b.floatValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Div implements Operator<Double, Number> {

        @Override
        public Object op(final Double a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a / b.intValue();
            } else if (claszB.equals(Long.class)) {
                return a / b.longValue();
            } else if (claszB.equals(Double.class)) {
                return a / ((Double) b);
            } else if (claszB.equals(BigInteger.class)) {
                return a / b.doubleValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return a / b.doubleValue();
            } else if (claszB.equals(Float.class)) {
                return a / b.floatValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mod implements Operator<Double, Number> {

        @Override
        public Object op(final Double a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.longValue() % b.intValue();
            } else if (claszB.equals(Long.class)) {
                return a.longValue() % b.longValue();
            } else if (claszB.equals(Double.class)) {
                return  a % b.longValue();
            } else if (claszB.equals(BigInteger.class)) {
                return BigInteger.valueOf(a.longValue()).mod((BigInteger) b).intValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return BigInteger.valueOf(a.longValue()).mod(((BigDecimal) b).toBigInteger()).intValue();
            } else if (claszB.equals(Float.class)) {
                return  a % b.longValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Pow implements Operator<Double, Number> {

        @Override
        public Object op(final Double a, final Number b) {
            return Math.pow(a, b.doubleValue());
        }
    }


}
