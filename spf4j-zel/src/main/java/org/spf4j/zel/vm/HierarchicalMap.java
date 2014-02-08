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

package org.spf4j.zel.vm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author zoly
 * @param <K>
 * @param <V>
 */
public final class HierarchicalMap<K, V>  implements Map<K, V> {
    
    private final List<Map<K, V>> maps;

    public HierarchicalMap(final HierarchicalMap<K, V> parentMap, final Map<K, V> map) {
        this.maps = new ArrayList<Map<K, V>>(parentMap.maps.size() + 1);
        this.maps.add(map);
        this.maps.addAll(parentMap.maps);
    }
    
    
    public HierarchicalMap(final Map<K, V> map) {
        this.maps = new ArrayList<Map<K, V>>(1);
        this.maps.add(map);
    }
    
    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsKey(final Object key) {
        for (Map<K, V> map : maps) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(final Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public V get(final Object key) {
        for (Map<K, V> map : maps) {
            if (map.containsKey((K) key)) {
                return map.get(key);
            }
        }
        return null;
    }

    @Override
    public V put(final K key, final V value) {
        return maps.get(0).put(key, value);
    }

    @Override
    public V remove(final Object key) {
        return maps.get(0).remove(key);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        maps.get(0).putAll(m);
    }

    @Override
    public void clear() {
        maps.get(0).clear();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
