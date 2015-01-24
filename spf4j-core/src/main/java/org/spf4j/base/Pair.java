
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

import org.spf4j.io.Csv;
import com.google.common.base.Objects;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public class Pair<A, B> implements Map.Entry<A, B> {

    public Pair(final A first, final B second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Pair<A, B> of(final A first, final B second) {
        return new Pair<>(first, second);
    }

    /**
     * Creates a pair from a (str1,str2) pair.
     *
     * @param stringPair
     * @return null if this is not a pair.
     */
    @Nullable
    public static Pair<String, String> from(final String stringPair) {
        if (!stringPair.startsWith(PREFIX) || !stringPair.endsWith(SUFFIX)) {
            return null;
        }
        int commaIdx = stringPair.indexOf(',');
        if (commaIdx < 0) {
            return null;
        }

        StringReader sr = new StringReader(
                stringPair.substring(PREFIX.length(), stringPair.length() - SUFFIX.length()));
        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        int comma;
        try {
            comma = Csv.readCsvElement(sr, first);
            if (comma != ',') {
                return null;
            }

            int last = Csv.readCsvElement(sr, second);
            if (last >= 0) {
                return null;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return Pair.of(first.toString(), second.toString());
    }
    private static final String SUFFIX = ")";

    private static final String PREFIX = "(";

    //CHECKSTYLE:OFF
    protected final A first;

    protected final B second;
    //CHECKSTYLE:ON

    public final A getFirst() {
        return first;
    }

    public final B getSecond() {
        return second;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pair<A, B> other = (Pair<A, B>) obj;
        if (this.first != other.first && (this.first == null || !this.first.equals(other.first))) {
            return false;
        }
        return (!(this.second != other.second && (this.second == null || !this.second.equals(other.second))));
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(first, second);
    }

    @Override
    public final String toString() {
        return PREFIX + Csv.toCsvElement(first.toString())
                + "," + Csv.toCsvElement(second.toString()) + ')';
    }

    public final List<Object> toList() {
        return java.util.Arrays.asList(first, second);
    }

    public static <K, V extends Object> Map<K, V> asMap(final Pair<K, ? extends V> ... pairs) {
        Map<K, V> result = new LinkedHashMap<>(pairs.length);
        for (Pair<K, ? extends V> pair : pairs) {
            result.put(pair.getFirst(), pair.getSecond());
        }
        return result;
    }

    @Override
    public final A getKey() {
        return first;
    }

    @Override
    public final B getValue() {
        return second;
    }

    @Override
    public final B setValue(final B value) {
        throw new UnsupportedOperationException("Object not mutable");
    }

}
