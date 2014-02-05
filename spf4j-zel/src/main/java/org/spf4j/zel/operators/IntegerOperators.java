/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.operators;

import com.google.common.math.IntMath;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.spf4j.zel.operators.Operator.MATH_CONTEXT;

public final class IntegerOperators {
    
    private IntegerOperators() {
    }

    public static final class Add implements Operator<Integer, Number> {

        @Override
        public Object op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                long result = (long) a + b.intValue();
                if (result == (int) result) {
                    return Integer.valueOf((int) result);
                } else {
                    return result;
                }
            } else if (claszB.equals(Long.class)) {
                long aa = a;
                long bb = (Long) b;
                long result = aa + bb;
                if ((aa ^ bb) < 0 | (aa ^ result) >= 0) {
                    return result;
                } else {
                    BigInteger rr = BigInteger.valueOf(bb);
                    return rr.add(BigInteger.valueOf(aa));
                }
            } else if (claszB.equals(Double.class)) {
                return ((Double) b) + a;
            } else if (claszB.equals(Float.class)) {
                return ((Double) b) + a;
            } else if (claszB.equals(BigInteger.class)) {
                return ((BigInteger) b).add(BigInteger.valueOf((long) a));
            } else if (claszB.equals(BigDecimal.class)) {
                return ((BigDecimal) b).add(BigDecimal.valueOf(a), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Sub implements Operator<Integer, Number> {

        @Override
        public Object op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                long result = (long) a - b.intValue();
                if (result == (int) result) {
                    return Integer.valueOf((int) result);
                } else {
                    return result;
                }
            } else if (claszB.equals(Long.class)) {
                long aa = a;
                long bb = (Long) b;
                long result = aa - bb;
                if ((aa ^ bb) < 0 | (aa ^ result) >= 0) {
                    return result;
                } else {
                    BigInteger rr = BigInteger.valueOf(bb);
                    return rr.add(BigInteger.valueOf(aa));
                }
            } else if (claszB.equals(Double.class)) {
                return (double) a - ((Double) b);
            } else if (claszB.equals(Float.class)) {
                return (double) a - ((Float) b);
            } else if (claszB.equals(BigInteger.class)) {
                return BigInteger.valueOf((long) a).subtract((BigInteger) b);
            } else if (claszB.equals(BigDecimal.class)) {
                return BigDecimal.valueOf((long) a).subtract((BigDecimal) b, MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mul implements Operator<Integer, Number> {

        @Override
        public Object op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                long result = (long) a * b.intValue();
                if (result == (int) result) {
                    return Integer.valueOf((int) result);
                } else {
                    return result;
                }
            } else if (claszB.equals(Long.class)) {
                long aa = a;
                long bb = (Long) b;
                int leadingZeros = Long.numberOfLeadingZeros(aa) + Long.numberOfLeadingZeros(~aa)
                        + Long.numberOfLeadingZeros(bb) + Long.numberOfLeadingZeros(~bb);
                if (leadingZeros > Long.SIZE + 1) {
                    return aa * bb;
                }
                if (!(leadingZeros >= Long.SIZE)) {
                    return BigInteger.valueOf(aa).multiply(BigInteger.valueOf(bb));
                }
                if (!(aa >= 0 | bb != Long.MIN_VALUE)) {
                    return BigInteger.valueOf(aa).multiply(BigInteger.valueOf(bb));
                }
                long tentativeResult = aa * bb;
                if (!(aa == 0 || tentativeResult / aa == bb)) {
                    return BigInteger.valueOf(aa).multiply(BigInteger.valueOf(bb));
                }
                return tentativeResult;
            } else if (claszB.equals(Double.class)) {
                return (double) a * ((Double) b);
            } else if (claszB.equals(Float.class)) {
                return (double) a * ((Float) b);
            } else if (claszB.equals(BigInteger.class)) {
                return BigInteger.valueOf((long) a).multiply((BigInteger) b);
            } else if (claszB.equals(BigDecimal.class)) {
                return BigDecimal.valueOf((long) a).multiply((BigDecimal) b, MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Div implements Operator<Integer, Number> {

        @Override
        public Object op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                long result = (long) a / b.intValue();
                return (int) result;
            } else if (claszB.equals(Long.class)) {
                long aa = a;
                long bb = (Long) b;
                return aa / bb;
            } else if (claszB.equals(Double.class)) {
                return (double) a / ((Double) b);
            } else if (claszB.equals(Float.class)) {
                return (double) a / ((Float) b);
            } else if (claszB.equals(BigInteger.class)) {
                return BigInteger.valueOf((long) a).divide((BigInteger) b).intValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return BigDecimal.valueOf((long) a).divide((BigDecimal) b, MATH_CONTEXT.get()).intValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mod implements Operator<Integer, Number> {

        @Override
        public Object op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                long result = (long) a % b.intValue();
                return (int) result;
            } else if (claszB.equals(Long.class)) {
                return a % b.longValue();
            } else if (claszB.equals(Double.class)) {
                return  a % b.longValue();
            } else if (claszB.equals(Float.class)) {
                return  a % b.longValue();
            } else if (claszB.equals(BigInteger.class)) {
                return BigInteger.valueOf((long) a).mod((BigInteger) b).intValue();
            } else if (claszB.equals(BigDecimal.class)) {
                return BigInteger.valueOf((long) a).mod(((BigDecimal) b).toBigInteger()).intValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Pow implements Operator<Integer, Number> {

        @Override
        public Object op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return powIntInt(a, b);
            } else if (claszB.equals(Long.class)) {
                if (((Long) b).compareTo((long) Integer.MAX_VALUE) > 0) {
                    return Math.pow(a, (Long) b);
                } else {
                    return powIntInt(a, b.intValue());
                }
            } else if (claszB.equals(Double.class)) {
                return Math.pow(a, (Double) b);
            } else if (claszB.equals(Float.class)) {
                return Math.pow(a, (Double) b);
            } else if (claszB.equals(BigInteger.class)) {
                return BigInteger.valueOf((long) a).pow(b.intValue());
            } else if (claszB.equals(BigDecimal.class)) {
                return BigDecimal.valueOf((long) a).pow(b.intValue());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    private static Object powIntInt(final Integer a, final Number b) {
        int result;
        try {
            result = IntMath.checkedPow(a, b.intValue());
        } catch (ArithmeticException e) {
            return BigInteger.valueOf(a).pow(b.intValue());
        }
        return result;
    }

}
