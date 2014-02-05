/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.operators;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public final class BigDecimalOperators {

    private BigDecimalOperators() {
    }

    
    public static final class Add implements Operator<BigDecimal, Number> {

        @Override
        public Object op(final BigDecimal a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.add(new BigDecimal(b.intValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(Long.class)) {
                return a.add(new BigDecimal(b.longValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(Double.class)) {
                return a.add(new BigDecimal(b.doubleValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(BigInteger.class)) {
                return a.add(new BigDecimal((BigInteger) b), MATH_CONTEXT.get());
            } else if (claszB.equals(BigDecimal.class)) {
                return a.add((BigDecimal) b, MATH_CONTEXT.get());
            } else if (claszB.equals(Float.class)) {
                return a.add(new BigDecimal(b.floatValue()), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Sub implements Operator<BigDecimal, Number> {

        @Override
        public Object op(final BigDecimal a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.subtract(new BigDecimal(b.intValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(Long.class)) {
                return a.subtract(new BigDecimal(b.longValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(Double.class)) {
                return a.subtract(new BigDecimal(b.doubleValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(BigInteger.class)) {
                return a.subtract(new BigDecimal((BigInteger) b), MATH_CONTEXT.get());
            } else if (claszB.equals(BigDecimal.class)) {
                return a.subtract((BigDecimal) b, MATH_CONTEXT.get());
            } else if (claszB.equals(Float.class)) {
                return a.subtract(new BigDecimal(b.floatValue()), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mul implements Operator<BigDecimal, Number> {

        @Override
        public Object op(final BigDecimal a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.multiply(new BigDecimal(b.intValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(Long.class)) {
                return a.multiply(new BigDecimal(b.longValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(Double.class)) {
                return a.multiply(new BigDecimal(b.doubleValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(BigInteger.class)) {
                return a.multiply(new BigDecimal((BigInteger) b), MATH_CONTEXT.get());
            } else if (claszB.equals(BigDecimal.class)) {
                return a.multiply((BigDecimal) b, MATH_CONTEXT.get());
            } else if (claszB.equals(Double.class)) {
                return a.multiply(new BigDecimal(b.doubleValue()), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Div implements Operator<BigDecimal, Number> {

        @Override
        public Object op(final BigDecimal a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.divide(new BigDecimal(b.intValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(Long.class)) {
                return a.divide(new BigDecimal(b.longValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(Double.class)) {
                return a.divide(new BigDecimal(b.doubleValue()), MATH_CONTEXT.get());
            } else if (claszB.equals(BigInteger.class)) {
                return a.divide(new BigDecimal((BigInteger) b), MATH_CONTEXT.get());
            } else if (claszB.equals(BigDecimal.class)) {
                return a.divide((BigDecimal) b, MATH_CONTEXT.get());
            } else if (claszB.equals(Float.class)) {
                return a.divide(new BigDecimal(b.doubleValue()), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mod implements Operator<BigDecimal, Number> {

        @Override
        public Object op(final BigDecimal a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.toBigInteger().mod(BigInteger.valueOf(b.intValue()));
            } else if (claszB.equals(Long.class)) {
                return a.toBigInteger().mod(BigInteger.valueOf(b.longValue()));
            } else if (claszB.equals(Double.class)) {
                return a.toBigInteger().mod(new BigDecimal(b.doubleValue()).toBigInteger());
            } else if (claszB.equals(BigInteger.class)) {
                return a.toBigInteger().mod((BigInteger) b);
            } else if (claszB.equals(BigDecimal.class)) {
                return a.toBigInteger().mod(((BigDecimal) b).toBigInteger());
            } else if (claszB.equals(Float.class)) {
                return a.toBigInteger().mod(new BigDecimal(b.doubleValue()).toBigInteger());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Pow implements Operator<BigDecimal, Number> {

        @Override
        public Object op(final BigDecimal a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a.pow(b.intValue(), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }

    }
}
