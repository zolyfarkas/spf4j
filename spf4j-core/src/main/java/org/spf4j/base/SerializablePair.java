
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({ "PIS_POSSIBLE_INCOMPLETE_SERIALIZATION", "SE_NO_SUITABLE_CONSTRUCTOR" })
public class SerializablePair<A extends Serializable, B extends Serializable>
        extends Pair<A, B>
        implements Serializable {

    private static final long serialVersionUID = 1L;

    public SerializablePair(final A first, final B second) {
        super(first, second);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("UMTP_UNBOUND_METHOD_TEMPLATE_PARAMETER")
    public static <A extends Serializable, B extends Serializable>
            SerializablePair<A, B> of(final A first, final B second) {
        return new SerializablePair<>(first, second);
    }

    private Object writeReplace()
            throws java.io.ObjectStreamException {
        PairProxy result = new PairProxy();
        result.first = getFirst();
        result.second = getSecond();
        return result;
    }

    private static final class PairProxy implements Serializable {

        private static final long serialVersionUID = 1L;
        private Serializable first;
        private Serializable second;

        private Object readResolve()
                throws java.io.ObjectStreamException {
            return SerializablePair.of(first, second);
        }

    }

}
