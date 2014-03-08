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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author zoly
 * @param <K>
 * @param <V>
 */
public final class HierarchicalMap<K, V>  implements Map<K, V> {
    
    private Map<K, V> map;
    
    private final HierarchicalMap<K, V> parentMap;

    
    
    public HierarchicalMap(final HierarchicalMap<K, V> parentMap, final Map<K, V> map) {
        this.map = map;
        this.parentMap = parentMap;
    }

    public HierarchicalMap(final HierarchicalMap<K, V> parentMap) {
        this.map = null;
        this.parentMap = parentMap;
    }
    
    
    public HierarchicalMap(final Map<K, V> map) {
        this.map = map;
        this.parentMap = null;
    }

    public Map<K, V> getMap() {
        return map;
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
        HierarchicalMap<K, V> imap = this;
        do {
            if (imap.map != null && imap.map.containsKey(key)) {
                return true;
            }
            imap = imap.parentMap;
        } while (imap != null);
        return false;
    }

    @Override
    public boolean containsValue(final Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public V get(final Object key) {
        HierarchicalMap<K, V> imap = this;
        do {
            final Map<K, V> rmap = imap.map;
            V result = rmap.get(key);
            if (result != null) {
                return result;
            }
            imap = imap.parentMap;
        } while (imap != null);
        return null;
    }

    @Override
    public V put(final K key, final V value) {
        if (map == null) {
            map = new HashMap<K, V>();
        }
        return map.put(key, value);
    }

    @Override
    public V remove(final Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        if (map == null) {
            map = new HashMap<K, V>();
        }
        map.putAll(m);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
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
