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
 * Stack of Maps implementation.
 * @author zoly
 */
public final class StackedMap<K,V>
        implements Map<K,V>
{

    private final SimpleStack<Map<K, V>> stack;

    public StackedMap()
    {
        stack = new SimpleStack<Map<K, V>>();
        stack.push(new HashMap<K, V>());
    }


    public void push()
    {
        stack.push(new HashMap<K, V>());
    }

    public Map<K,V> pop()
    {
        return stack.pop();
    }



    @Override
    public void putAll(Map m)
    {
        stack.peek().putAll(m);
    }

    @Override
    public void clear()
    {
        stack.clear();
        stack.push(new HashMap());
    }

    @Override
    public Set keySet()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection values()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set entrySet()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public V get(final Object key)
    {
        int i = stack.getPtr() - 1;
        while (i >= 0)
        {
            Map<K, V> m = stack.getFromPtr(i);
            @SuppressWarnings("element-type-mismatch")
            V o = m.get(key);
            if (o != null)
            {
                return o;
            }
            i--;
        }
        return null;
    }

    @Override
    public int size()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEmpty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public V put(final K key, final V value)
    {
        return stack.peek().put(key, value);
    }

    @Override
    public boolean containsKey(final Object key)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsValue(final Object value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public V remove(final Object key)
    {
        return stack.peek().remove(key);
    }

}

