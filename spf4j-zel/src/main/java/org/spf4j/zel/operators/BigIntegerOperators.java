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

import java.math.BigDecimal;
import java.math.BigInteger;
import static org.spf4j.zel.operators.Operator.MATH_CONTEXT;

public final class BigIntegerOperators {
    
    private BigIntegerOperators() {
    }

    public static final class Add implements Operator<BigInteger, Number, Number> {

        @Override
        public Number op(final BigInteger a, final Number b) {
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

    public static final class Sub implements Operator<BigInteger, Number, Number> {

        @Override
        public Number op(final BigInteger a, final Number b) {
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

    public static final class Mul implements Operator<BigInteger, Number, Number> {

        @Override
        public Number op(final BigInteger a, final Number b) {
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

    public static final class Div implements Operator<BigInteger, Number, Number> {

        @Override
        public Number op(final BigInteger a, final Number b) {
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

    public static final class Mod implements Operator<BigInteger, Number, Number> {

        @Override
        public Number op(final BigInteger a, final Number b) {
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

    public static final class Pow implements Operator<BigInteger, Number, Number> {

        @Override
        public Number op(final BigInteger a, final Number b) {
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
