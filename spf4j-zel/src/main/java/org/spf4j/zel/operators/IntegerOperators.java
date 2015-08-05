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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.spf4j.zel.operators.Operator.MATH_CONTEXT;

@SuppressFBWarnings({ "NS_NON_SHORT_CIRCUIT", "SIC_INNER_SHOULD_BE_STATIC_ANON" })
public final class IntegerOperators {

    private IntegerOperators() {
    }

    public static final class Add extends AbstractOps<Integer> {

        public Add() {
            super();
            Operator<Integer, Number, Number> isbc = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    long result = (long) a + b.intValue();
                    if (result == (int) result) {
                        return (int) result;
                    } else {
                        return result;
                    }
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    long aa = a;
                    long bb = (Long) b;
                    long result = aa + bb;
                    if ((aa ^ bb) < 0 | (aa ^ result) >= 0) {
                        return result;
                    } else {
                        BigInteger rr = BigInteger.valueOf(bb);
                        return rr.add(BigInteger.valueOf(aa));
                    }
                }
            });
            final Operator<Integer, Number, Number> dfOp = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return b.doubleValue() + a;
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return ((BigInteger) b).add(BigInteger.valueOf((long) a));
                }
            });
            operations.put(BigDecimal.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return ((BigDecimal) b).add(BigDecimal.valueOf(a), MATH_CONTEXT.get());
                }
            });
        }

    }

    public static final class Sub extends AbstractOps<Integer> {

        public Sub() {
            super();
            Operator<Integer, Number, Number> isbc = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    long result = (long) a - b.intValue();
                    if (result == (int) result) {
                        return (int) result;
                    } else {
                        return result;
                    }
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    long aa = a;
                    long bb = (Long) b;
                    long result = aa - bb;
                    if ((aa ^ bb) < 0 | (aa ^ result) >= 0) {
                        return result;
                    } else {
                        BigInteger rr = BigInteger.valueOf(bb);
                        return rr.add(BigInteger.valueOf(aa));
                    }
                }
            });
            final Operator<Integer, Number, Number> dfOp = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return (double) a - b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigInteger.valueOf((long) a).subtract((BigInteger) b);
                }
            });
            operations.put(BigDecimal.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigDecimal.valueOf((long) a).subtract((BigDecimal) b, MATH_CONTEXT.get());
                }
            });

        }

    }

    public static final class Mul extends AbstractOps<Integer> {

        public Mul() {
            super();
            Operator<Integer, Number, Number> isbc = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    long result = (long) a * b.intValue();
                    if (result == (int) result) {
                        return (int) result;
                    } else {
                        return result;
                    }
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
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
                }
            });
            final Operator<Integer, Number, Number> dfOp = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return (double) a * b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigInteger.valueOf((long) a).multiply((BigInteger) b);
                }
            });
            operations.put(BigDecimal.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigDecimal.valueOf((long) a).multiply((BigDecimal) b, MATH_CONTEXT.get());
                }
            });
        }
    }

    public static final class Div extends AbstractOps<Integer> {

        public Div() {
            super();
            Operator<Integer, Number, Number> isbc = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return (int) ((long) a / b.intValue());
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    long aa = a;
                    long bb = (Long) b;
                    return aa / bb;
                }
            });
            final Operator<Integer, Number, Number> dfOp = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return (double) a / b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigInteger.valueOf((long) a).divide((BigInteger) b).intValue();
                }
            });
            operations.put(BigDecimal.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigDecimal.valueOf((long) a).divide((BigDecimal) b, MATH_CONTEXT.get());
                }
            });
        }
    }

    public static final class Mod extends AbstractOps<Integer> {

        public Mod() {
           super();
           Operator<Integer, Number, Number> isbc = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    long result = (long) a % b.intValue();
                    return (int) result;
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return a % b.longValue();
                }
            });
            final Operator<Integer, Number, Number> dfOp = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return  a % b.longValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigInteger.valueOf((long) a).mod((BigInteger) b).intValue();
                }
            });
            operations.put(BigDecimal.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigInteger.valueOf((long) a).mod(((BigDecimal) b).toBigInteger()).intValue();
                }
            });
        }
    }

    public static final class Pow extends AbstractOps<Integer> {

        public Pow() {
            super();
           Operator<Integer, Number, Number> isbc = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return powIntInt(a, b);
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    if (((Long) b).compareTo((long) Integer.MAX_VALUE) > 0) {
                        return Math.pow(a, (Long) b);
                    } else {
                        return powIntInt(a, b.intValue());
                    }
                }
            });
            final Operator<Integer, Number, Number> dfOp = new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                   return Math.pow(a, b.doubleValue());
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigInteger.valueOf((long) a).pow(b.intValue());
                }
            });
            operations.put(BigDecimal.class, new Operator<Integer, Number, Number>() {

                @Override
                public Number op(final Integer a, final Number b) {
                    return BigDecimal.valueOf((long) a).pow(b.intValue());
                }
            });
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
