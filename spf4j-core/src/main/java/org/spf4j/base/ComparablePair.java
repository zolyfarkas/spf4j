
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

import java.io.Serializable;

/**
 *
 * @author zoly
 */
public final class ComparablePair<A extends Comparable & Serializable, B extends Comparable & Serializable>
    extends SerializablePair<A, B>
    implements Comparable<ComparablePair<A, B>>, Serializable  {

    private static final long serialVersionUID = 1L;

    public ComparablePair(final A first, final B second) {
        super(first, second);
    }
    
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("UMTP_UNBOUND_METHOD_TEMPLATE_PARAMETER")
    public static <A extends Comparable & Serializable, B extends Comparable & Serializable>
                    ComparablePair<A, B> of(final A first, final B second) {
        return new ComparablePair<>(first, second);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public int compareTo(final ComparablePair<A, B> o) {
        if (this.first.equals(o.first)) {
            return this.second.compareTo(o.second);
        } else {
            return this.first.compareTo(o.first);
        }
    }
    
}
