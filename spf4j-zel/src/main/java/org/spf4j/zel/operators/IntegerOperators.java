/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.zel.operators;

import com.google.common.math.IntMath;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.spf4j.zel.operators.Operator.MATH_CONTEXT;

public final class IntegerOperators {
    
    private IntegerOperators() {
    }

    public static final class Add implements Operator<Integer, Number, Number> {

        @Override
        public Number op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB == Integer.class || claszB == Short.class
                    || claszB == Byte.class) {
                long result = (long) a + b.intValue();
                if (result == (int) result) {
                    return (int) result;
                } else {
                    return result;
                }
            } else if (claszB == Long.class) {
                long aa = a;
                long bb = (Long) b;
                long result = aa + bb;
                if ((aa ^ bb) < 0 | (aa ^ result) >= 0) {
                    return result;
                } else {
                    BigInteger rr = BigInteger.valueOf(bb);
                    return rr.add(BigInteger.valueOf(aa));
                }
            } else if (claszB == Double.class) {
                return ((Double) b) + a;
            } else if (claszB == Float.class) {
                return ((Double) b) + a;
            } else if (claszB == BigInteger.class) {
                return ((BigInteger) b).add(BigInteger.valueOf((long) a));
            } else if (claszB == BigDecimal.class) {
                return ((BigDecimal) b).add(BigDecimal.valueOf(a), MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Sub implements Operator<Integer, Number, Number> {

        @Override
        public Number op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB == Integer.class || claszB == Short.class
                    || claszB == Byte.class) {
                long result = (long) a - b.intValue();
                if (result == (int) result) {
                    return (int) result;
                } else {
                    return result;
                }
            } else if (claszB == Long.class) {
                long aa = a;
                long bb = (Long) b;
                long result = aa - bb;
                if ((aa ^ bb) < 0 | (aa ^ result) >= 0) {
                    return result;
                } else {
                    BigInteger rr = BigInteger.valueOf(bb);
                    return rr.add(BigInteger.valueOf(aa));
                }
            } else if (claszB == Double.class) {
                return (double) a - ((Double) b);
            } else if (claszB == Float.class) {
                return (double) a - ((Float) b);
            } else if (claszB == BigInteger.class) {
                return BigInteger.valueOf((long) a).subtract((BigInteger) b);
            } else if (claszB == BigDecimal.class) {
                return BigDecimal.valueOf((long) a).subtract((BigDecimal) b, MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mul implements Operator<Integer, Number, Number> {

        @Override
        public Number op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB == Integer.class || claszB == Short.class
                    || claszB == Byte.class) {
                long result = (long) a * b.intValue();
                if (result == (int) result) {
                    return (int) result;
                } else {
                    return result;
                }
            } else if (claszB == Long.class) {
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
            } else if (claszB == Double.class) {
                return (double) a * ((Double) b);
            } else if (claszB == Float.class) {
                return (double) a * ((Float) b);
            } else if (claszB == BigInteger.class) {
                return BigInteger.valueOf((long) a).multiply((BigInteger) b);
            } else if (claszB == BigDecimal.class) {
                return BigDecimal.valueOf((long) a).multiply((BigDecimal) b, MATH_CONTEXT.get());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Div implements Operator<Integer, Number, Number> {

        @Override
        public Number op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB == Integer.class || claszB == Short.class
                    || claszB == Byte.class) {
                long result = (long) a / b.intValue();
                return (int) result;
            } else if (claszB == Long.class) {
                long aa = a;
                long bb = (Long) b;
                return aa / bb;
            } else if (claszB == Double.class) {
                return (double) a / ((Double) b);
            } else if (claszB == Float.class) {
                return (double) a / ((Float) b);
            } else if (claszB == BigInteger.class) {
                return BigInteger.valueOf((long) a).divide((BigInteger) b).intValue();
            } else if (claszB == BigDecimal.class) {
                return BigDecimal.valueOf((long) a).divide((BigDecimal) b, MATH_CONTEXT.get()).intValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Mod implements Operator<Integer, Number, Number> {

        @Override
        public Number op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB == Integer.class || claszB == Short.class
                    || claszB == Byte.class) {
                long result = (long) a % b.intValue();
                return (int) result;
            } else if (claszB == Long.class) {
                return a % b.longValue();
            } else if (claszB == Double.class) {
                return  a % b.longValue();
            } else if (claszB == Float.class) {
                return  a % b.longValue();
            } else if (claszB == BigInteger.class) {
                return BigInteger.valueOf((long) a).mod((BigInteger) b).intValue();
            } else if (claszB == BigDecimal.class) {
                return BigInteger.valueOf((long) a).mod(((BigDecimal) b).toBigInteger()).intValue();
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    public static final class Pow implements Operator<Integer, Number, Number> {

        @Override
        public Number op(final Integer a, final Number b) {
            Class<? extends Number> claszB = b.getClass();
            if (claszB == Integer.class || claszB == Short.class
                    || claszB == Byte.class) {
                return powIntInt(a, b);
            } else if (claszB == Long.class) {
                if (((Long) b).compareTo((long) Integer.MAX_VALUE) > 0) {
                    return Math.pow(a, (Long) b);
                } else {
                    return powIntInt(a, b.intValue());
                }
            } else if (claszB == Double.class) {
                return Math.pow(a, (Double) b);
            } else if (claszB == Float.class) {
                return Math.pow(a, (Double) b);
            } else if (claszB == BigInteger.class) {
                return BigInteger.valueOf((long) a).pow(b.intValue());
            } else if (claszB == BigDecimal.class) {
                return BigDecimal.valueOf((long) a).pow(b.intValue());
            } else {
                throw new IllegalArgumentException(b.toString());
            }
        }
    }

    private static Number powIntInt(final Integer a, final Number b) {
        int result;
        try {
            result = IntMath.checkedPow(a, b.intValue());
        } catch (ArithmeticException e) {
            return BigInteger.valueOf(a).pow(b.intValue());
        }
        return result;
    }

}
