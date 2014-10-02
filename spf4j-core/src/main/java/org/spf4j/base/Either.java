
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
package org.spf4j.base;

/**
 * @author zoly
 */
public abstract class Either<A, B> {

    //CHECKSTYLE:OFF
    protected final Object value;
    //CHECKSTYLE:ON
    
    private Either(final Object value) {
        this.value = value;
    }
    
    public abstract boolean isLeft();
    
    
    public boolean isRight() {
        return !isLeft();
    }
    
    
    public abstract  A getLeft();
    
    public abstract  B getRight();

    @Override
    public int hashCode() {
        return 73 + (this.value != null ? this.value.hashCode() : 0);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Either<?, ?> other = (Either<?, ?>) obj;
        return (!(this.value != other.value && (this.value == null || !this.value.equals(other.value))));
    }

    
    
    @Override
    public String toString() {
        return "Either{" + "value=" + value + '}';
    }

    
    public static final class Left<A, B> extends Either<A, B> {

        public Left(final A a) {
            super(a);
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public A getLeft() {
            return (A) this.value;
        }

        @Override
        public B getRight() {
            throw new UnsupportedOperationException("This union doe not have a left val, instead a " + value);
        }
        
    }

    public static final class Right<A, B> extends Either<A, B> {
        
        public Right(final B b) {
           super(b);
        }

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public A getLeft() {
            throw new UnsupportedOperationException("This union doe not have a left val, instead a " + value);
        }

        @Override
        public B getRight() {
            return (B) value;
        }
    }

    public static <A, B> Either<A, B> left(final A a) {
        return new Left<A, B>(a);
    }
    
    public static <A, B> Either<A, B> right(final B b) {
        return new Right<A, B>(b);
    }

}
