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

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class DoubleOperators {

    private DoubleOperators() {
    }

    public static final class Add extends AbstractOps<Double> {

        public Add() {
            super();
            Operator<Double, Number, Number> isbc = new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return  a + b.intValue();
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return a + b.longValue();
                }
            });
            final Operator<Double, Number, Number> dfOp = new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return a + b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, dfOp);
            operations.put(BigDecimal.class, dfOp);

        }

    }

    public static final class Sub extends AbstractOps<Double> {

        public Sub() {
            super();
           Operator<Double, Number, Number> isbc = new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return  a - b.intValue();
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return a - b.longValue();
                }
            });
            final Operator<Double, Number, Number> dfOp = new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return a - b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, dfOp);
            operations.put(BigDecimal.class, dfOp);
        }
    }

    public static final class Mul extends AbstractOps<Double> {

        public Mul() {
           super();
           Operator<Double, Number, Number> isbc = new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return  a * b.intValue();
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return a * b.longValue();
                }
            });
            final Operator<Double, Number, Number> dfOp = new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return a * b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, dfOp);
            operations.put(BigDecimal.class, dfOp);
        }

    }

    public static final class Div extends AbstractOps<Double> {

        public Div() {
            super();
           Operator<Double, Number, Number> isbc = new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return  a / b.intValue();
                }
            };
            operations.put(Integer.class, isbc);
            operations.put(Byte.class, isbc);
            operations.put(Character.class, isbc);
            operations.put(Short.class, isbc);
            operations.put(Long.class, new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return a / b.longValue();
                }
            });
            final Operator<Double, Number, Number> dfOp = new Operator<Double, Number, Number>() {

                @Override
                public Number op(final Double a, final Number b) {
                    return a / b.doubleValue();
                }
            };
            operations.put(Double.class, dfOp);
            operations.put(Float.class, dfOp);
            operations.put(BigInteger.class, dfOp);
            operations.put(BigDecimal.class, dfOp);
        }
    }

    public static final class Mod extends AbstractOps<Double> {


    }

    public static final class Pow extends AbstractOps<Double> {

        @Override
        public Number op(final Double a, final Number b) {
            return Math.pow(a, b.doubleValue());
        }
    }


}
