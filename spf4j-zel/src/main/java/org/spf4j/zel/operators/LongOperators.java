/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.operators;

import com.google.common.math.LongMath;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.spf4j.zel.operators.Operator.MATH_CONTEXT;

@SuppressFBWarnings({"NS_DANGEROUS_NON_SHORT_CIRCUIT", "NS_NON_SHORT_CIRCUIT" })
public final class LongOperators {
    
    private LongOperators() {
    }

    public static final class Add implements Operator<Long, Number, Number> {

        @Override
        public Number op(final Long pa, final Number b) {
            final long a = pa;
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                long bb = b.longValue();
                long result = a + bb;
                if ((a ^ bb) < 0 | (a ^ result) >= 0) {
                    return result;
                } else {
                    return BigInteger.valueOf(a).add(BigInteger.valueOf(bb));
                }
            } else if (claszB.equals(Double.class)) {
                return ((Double) b) + a;
            } else if (claszB.equals(Float.class)) {
                return ((Double) b) + a;
            } else if (claszB.equals(BigInteger.class)) {
                return ((BigInteger) b).add(BigInteger.valueOf(a));
            } else if (claszB.equals(BigDecimal.class)) {
                return ((BigDecimal) b).add(BigDecimal.valueOf(a), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Sub implements Operator<Long, Number, Number> {

        @Override
        public Number op(final Long pa, final Number b) {
            final long a = pa;
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                long bb = b.longValue();
                long result = a - bb;
                if ((a ^ bb) < 0 | (a ^ result) >= 0) {
                    return result;
                } else {
                    return BigInteger.valueOf(a).subtract(BigInteger.valueOf(bb));
                }
            } else if (claszB.equals(Double.class)) {
                return (double) a - ((Double) b);
            } else if (claszB.equals(Float.class)) {
                return (double) a - ((Float) b);
            } else if (claszB.equals(BigInteger.class)) {
                return BigInteger.valueOf(a).subtract((BigInteger) b);
            } else if (claszB.equals(BigDecimal.class)) {
                return BigDecimal.valueOf(a).subtract((BigDecimal) b, MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mul implements Operator<Long, Number, Number> {

        @Override
        public Number op(final Long pa, final Number b) {
            final long a = pa;
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                
                long bb = b.longValue();
                int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a)
                        + Long.numberOfLeadingZeros(bb) + Long.numberOfLeadingZeros(~bb);

                if (leadingZeros > Long.SIZE + 1) {
                    return a * bb;
                }
                if (!(leadingZeros >= Long.SIZE)) {
                    return BigInteger.valueOf(a).multiply(BigInteger.valueOf(bb));
                }
                if (!(a >= 0 | bb != Long.MIN_VALUE)) {
                    return BigInteger.valueOf(a).multiply(BigInteger.valueOf(bb));
                }
                long result = a * bb;
                if (!(a == 0 || result / a == bb)) {
                    return BigInteger.valueOf(a).multiply(BigInteger.valueOf(bb));
                }
                return result;
            } else if (claszB.equals(Double.class)) {
                return (double) a * ((Double) b);
            } else if (claszB.equals(Float.class)) {
                return (double) a * ((Float) b);
            } else if (claszB.equals(BigInteger.class)) {
                return BigInteger.valueOf(a).multiply((BigInteger) b);
            } else if (claszB.equals(BigDecimal.class)) {
                return BigDecimal.valueOf(a).multiply((BigDecimal) b, MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Div implements Operator<Long, Number, Number> {

        @Override
        public Number op(final Long pa, final Number b) {
            long a = pa;
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return a / b.longValue();
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

    public static final class Mod implements Operator<Long, Number, Number> {

        @Override
        public Number op(final Long pa, final Number b) {
            long a = pa;
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class) || claszB.equals(Long.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
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

    public static final class Pow implements Operator<Long, Number, Number> {

        @Override
        public Number op(final Long pa, final Number b) {
            long a = pa;
            Class<? extends Number> claszB = b.getClass();
            if (claszB.equals(Integer.class) || claszB.equals(Short.class)
                    || claszB.equals(Byte.class) || claszB.equals(Character.class)) {
                return powLongInt(a, b);
            } else if (claszB.equals(Long.class)) {
                if (((Long) b).compareTo((long) Integer.MAX_VALUE) > 0) {
                    return Math.pow(a, (Long) b);
                } else {
                    return powLongInt(a, b.intValue());
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

    private static Number powLongInt(final Long a, final Number b) {
        long result;
        try {
            result = LongMath.checkedPow(a, b.intValue());
        } catch (ArithmeticException e) {
            return BigInteger.valueOf(a).pow(b.intValue());
        }
        return result;
    }

}
