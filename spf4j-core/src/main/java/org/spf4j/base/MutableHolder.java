
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

import java.util.Objects;

/**
 *
 * @author zoly
 */
public final  class MutableHolder<T> implements Comparable<MutableHolder<T>> {

    private T value;

    public MutableHolder(final T value) {
        this.value = value;
    }

    public MutableHolder() {
        this.value = null;
    }

    public static <T> MutableHolder<T> of(final T value) {
      return (MutableHolder<T>) new MutableHolder<>(value);
    }

    public T getValue() {
        return value;
    }

    public void setValue(final T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MutableHolder{" + "value=" + value + '}';
    }

    @Override
    public int compareTo(final MutableHolder<T> o) {
        if (this.value == null) {
            if (o.value == null) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (o.value == null) {
                return 1;
            } else {
                return ((Comparable) this.value).compareTo(o.value);
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return 79 * hash + Objects.hashCode(this.value);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MutableHolder<?> other = (MutableHolder<?>) obj;
        return Objects.equals(this.value, other.value);
    }




}
