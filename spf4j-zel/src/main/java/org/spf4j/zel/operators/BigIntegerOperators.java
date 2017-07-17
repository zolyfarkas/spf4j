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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.spf4j.zel.operators.Operator.MATH_CONTEXT;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class BigIntegerOperators {

    private BigIntegerOperators() {
    }

    public static final class Add extends AbstractOps<BigInteger> {

        public Add() {
            super();
            Operator<BigInteger, Number, Number> isbc = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                   return a.add(BigInteger.valueOf(b.longValue()));
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<BigInteger, Number, Number> dfOp = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return b.doubleValue() + a.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return ((BigInteger) b).add(a);
                }
            });
            operations.put(BigDecimal.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return ((BigDecimal) b).add(new BigDecimal(a), MATH_CONTEXT.get());
                }
            });

        }
    }

    public static final class Sub extends AbstractOps<BigInteger> {

        public Sub() {
            super();
            Operator<BigInteger, Number, Number> isbc = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                   return a.subtract(BigInteger.valueOf(b.longValue()));
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<BigInteger, Number, Number> dfOp = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.doubleValue() - b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.subtract((BigInteger) b);
                }
            });
            operations.put(BigDecimal.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return new BigDecimal(a).subtract((BigDecimal) b, MATH_CONTEXT.get());
                }
            });

        }

    }

    public static final class Mul extends AbstractOps<BigInteger> {

        public Mul() {
            super();
            Operator<BigInteger, Number, Number> isbc = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                   return a.multiply(BigInteger.valueOf(b.longValue()));
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<BigInteger, Number, Number> dfOp = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.doubleValue() * b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.multiply((BigInteger) b);
                }
            });
            operations.put(BigDecimal.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return new BigDecimal(a).multiply((BigDecimal) b, MATH_CONTEXT.get());
                }
            });
        }
    }

    public static final class Div extends AbstractOps<BigInteger> {

        public Div() {
            super();
            Operator<BigInteger, Number, Number> isbc = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                   return a.divide(BigInteger.valueOf(b.longValue()));
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<BigInteger, Number, Number> dfOp = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.doubleValue() / b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.divide((BigInteger) b).intValue();
                }
            });
            operations.put(BigDecimal.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return new BigDecimal(a).divide((BigDecimal) b, MATH_CONTEXT.get()).intValue();
                }
            });
        }
    }

    public static final class Mod extends AbstractOps<BigInteger>  {

        public Mod() {
            super();
            Operator<BigInteger, Number, Number> isbc = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                   return a.mod(BigInteger.valueOf(b.longValue()));
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, isbc);
            final Operator<BigInteger, Number, Number> dfOp = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.mod(BigInteger.valueOf(b.longValue()));
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.mod((BigInteger) b).intValue();
                }
            });
            operations.put(BigDecimal.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return a.mod(((BigDecimal) b).toBigInteger()).intValue();
                }
            });
        }
    }

    public static final class Pow extends AbstractOps<BigInteger> {

        public Pow() {
            super();
            Operator<BigInteger, Number, Number> isbc = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                   return a.pow(b.intValue());
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    if (((Long) b).compareTo((long) Integer.MAX_VALUE) > 0) {
                        return Math.pow(a.doubleValue(), (Long) b);
                    } else {
                        return a.pow(b.intValue());
                    }
                }
            });
            final Operator<BigInteger, Number, Number> dfOp = new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return Math.pow(a.doubleValue(), b.doubleValue());
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return Math.pow(a.doubleValue(), b.doubleValue());
                }
            });
            operations.put(BigDecimal.class, new Operator<BigInteger, Number, Number>() {

                @Override
                public Number op(final BigInteger a, final Number b) {
                    return Math.pow(a.doubleValue(), b.doubleValue());
                }
            });
        }
    }
}
