
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
 *
 * @author zoly
 */
public final  class Holder<T> {

    private final T value;

    public Holder(final T value) {
        this.value = value;
    }

    public Holder() {
        this.value = null;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Holder{" + "value=" + value + '}';
    }

    public static <T> Holder<T> of(final T value) {
        return new Holder<>(value);
    }

    public static final Holder OF_NULL = Holder.of(null);

}
