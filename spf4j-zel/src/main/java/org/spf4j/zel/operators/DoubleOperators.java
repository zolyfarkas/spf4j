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

public final class DoubleOperators {
    
    private DoubleOperators() {
    }

    public static final class Add implements Operator<Double, Number, Number> {

        @Override
        public Number op(final Double a, final Number b) {
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

    public static final class Sub implements Operator<Double, Number, Number> {

        @Override
        public Number op(final Double a, final Number b) {
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

    public static final class Mul implements Operator<Double, Number, Number> {

        @Override
        public Number op(final Double a, final Number b) {
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

    public static final class Div implements Operator<Double, Number, Number> {

        @Override
        public Number op(final Double a, final Number b) {
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

    public static final class Mod implements Operator<Double, Number, Number> {

        @Override
        public Number op(final Double a, final Number b) {
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

    public static final class Pow implements Operator<Double, Number, Number> {

        @Override
        public Number op(final Double a, final Number b) {
            return Math.pow(a, b.doubleValue());
        }
    }


}
