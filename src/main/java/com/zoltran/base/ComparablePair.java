/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.base;

import com.google.common.base.Objects;

/**
 *
 * @author zoly
 */
public final class ComparablePair<A extends Comparable,B extends Comparable>
    implements Comparable<ComparablePair<A,B>> {

    public ComparablePair(A first, B second) {
        this.first = first;
        this.second = second;
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
    public boolean equals(Object obj) {
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
        return Objects.hashCode(first,second);
    }

    @Override
    public String toString() {
        return "Pair{" + "first=" + first + ", second=" + second + '}';
    }

    public int compareTo(ComparablePair<A, B> o) {
        if (this.first.equals(o.first))
            return this.second.compareTo(o.second);
        else 
            return this.first.compareTo(o.first);
    }
    
    
    
}
