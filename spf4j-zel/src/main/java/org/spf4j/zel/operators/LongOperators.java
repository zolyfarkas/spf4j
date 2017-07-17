/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.zel.operators;

import com.google.common.math.LongMath;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.spf4j.zel.operators.Operator.MATH_CONTEXT;

@SuppressFBWarnings({"SIC_INNER_SHOULD_BE_STATIC_ANON" })
public final class LongOperators {

    private LongOperators() {
    }

    public static final class Add extends AbstractOps<Long> {

        public Add() {
            super();
            Operator<Long, Number, Number> isbc = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long pa, final Number b) {
                    long a = pa;
                    long bb = b.longValue();
                    long result = a + bb;
                    if ((a ^ bb) < 0 || (a ^ result) >= 0) {
                        return result;
                    } else {
                        return BigInteger.valueOf(a).add(BigInteger.valueOf(bb));
                    }
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<Long, Number, Number> dfOp = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return b.doubleValue() + a;
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return ((BigInteger) b).add(BigInteger.valueOf(a));
                }
            });
            operations.put(BigDecimal.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return ((BigDecimal) b).add(BigDecimal.valueOf(a), MATH_CONTEXT.get());
                }
            });
        }
    }

    public static final class Sub extends AbstractOps<Long> {

        public Sub() {
            super();
            Operator<Long, Number, Number> isbc = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long pa, final Number b) {
                    long a = pa;
                    long bb = b.longValue();
                    long result = a - bb;
                    if ((a ^ bb) < 0 || (a ^ result) >= 0) {
                        return result;
                    } else {
                        return BigInteger.valueOf(a).subtract(BigInteger.valueOf(bb));
                    }
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<Long, Number, Number> dfOp = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return (double) a - b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigInteger.valueOf(a).subtract((BigInteger) b);
                }
            });
            operations.put(BigDecimal.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigDecimal.valueOf(a).subtract((BigDecimal) b, MATH_CONTEXT.get());
                }
            });
        }
    }

    public static final class Mul extends AbstractOps<Long> {

        public Mul() {
            super();
            Operator<Long, Number, Number> isbc = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long pa, final Number b) {
                    long a = pa;
                    long bb = b.longValue();
                    int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a)
                            + Long.numberOfLeadingZeros(bb) + Long.numberOfLeadingZeros(~bb);

                    if (leadingZeros > Long.SIZE + 1) {
                        return a * bb;
                    }
                    if (leadingZeros < Long.SIZE) {
                        return BigInteger.valueOf(a).multiply(BigInteger.valueOf(bb));
                    }
                    if (!(a >= 0 || bb != Long.MIN_VALUE)) {
                        return BigInteger.valueOf(a).multiply(BigInteger.valueOf(bb));
                    }
                    long result = a * bb;
                    if (!(a == 0 || result / a == bb)) {
                        return BigInteger.valueOf(a).multiply(BigInteger.valueOf(bb));
                    }
                    return result;
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<Long, Number, Number> dfOp = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return (double) a * b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigInteger.valueOf(a).multiply((BigInteger) b);
                }
            });
            operations.put(BigDecimal.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigDecimal.valueOf(a).multiply((BigDecimal) b, MATH_CONTEXT.get());
                }
            });
        }
    }

    public static final class Div extends AbstractOps<Long> {

        public Div() {
            super();
            Operator<Long, Number, Number> isbc = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return a / b.longValue();
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<Long, Number, Number> dfOp = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                     return (double) a / b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigInteger.valueOf((long) a).divide((BigInteger) b).intValue();
                }
            });
            operations.put(BigDecimal.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigDecimal.valueOf((long) a).divide((BigDecimal) b, MATH_CONTEXT.get());
                }
            });

        }
    }

    public static final class Mod extends AbstractOps<Long> {

        public Mod() {
            super();
            Operator<Long, Number, Number> isbc = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return a % b.longValue();
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<Long, Number, Number> dfOp = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                     return  a % b.longValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigInteger.valueOf((long) a).mod((BigInteger) b).longValue();
                }
            });
            operations.put(BigDecimal.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigInteger.valueOf((long) a).mod(((BigDecimal) b).toBigInteger()).longValue();
                }
            });
        }
    }

    public static final class Pow extends AbstractOps<Long> {

        public Pow() {
            super();
            Operator<Long, Number, Number> isbc = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                   return powLongInt(a, b);
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    if (((Long) b).compareTo((long) Integer.MAX_VALUE) > 0) {
                        return Math.pow(a, (Long) b);
                    } else {
                        return powLongInt(a, b.intValue());
                    }
                }
            });
            final Operator<Long, Number, Number> dfOp = new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                     return Math.pow(a, b.doubleValue());
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigInteger.valueOf((long) a).pow(b.intValue());
                }
            });
            operations.put(BigDecimal.class, new Operator<Long, Number, Number>() {

                @Override
                public Number op(final Long a, final Number b) {
                    return BigDecimal.valueOf((long) a).pow(b.intValue());
                }
            });

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
