
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

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author zoly
 */
public final class ComparablePair<A extends Comparable & Serializable, B extends Comparable & Serializable>
    implements Comparable<ComparablePair<A, B>> {

    public ComparablePair(final A first, final B second) {
        this.first = first;
        this.second = second;
    }
    
    public static <A extends Comparable & Serializable, B extends Comparable & Serializable> ComparablePair<A, B> 
            of(final A first, final B second) {
        return new ComparablePair<A, B>(first, second);
    }
    
    
    private final A first;
    
    private final B second;

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ComparablePair<A, B> other = (ComparablePair<A, B>) obj;
        if (this.first != other.first && (this.first == null || !this.first.equals(other.first))) {
            return false;
        }
        if (this.second != other.second && (this.second == null || !this.second.equals(other.second))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(first, second);
    }

    @Override
    public String toString() {
        return "Pair{" + "first=" + first + ", second=" + second + '}';
    }

    @Override
    public int compareTo(final ComparablePair<A, B> o) {
        if (this.first.equals(o.first)) {
            return this.second.compareTo(o.second);
        } else {
            return this.first.compareTo(o.first);
        }
    }
    
    public List<? extends Comparable> toList() {
        return java.util.Arrays.asList(first, second);
    }
    
    
}
