
package org.spf4j.ds;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author zoly
 */
public final class LinkedHashSetEx<V> extends AbstractSet<V>
    implements LinkedSet<V> {


    private final LinkedMap<V, Object> map;

    public LinkedHashSetEx() {
        map = new LinkedHashMapEx<>();
    }

    public LinkedHashSetEx(final int capacity) {
        map = new LinkedHashMapEx<>(capacity);
    }

    @Override
    public Iterator<V> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public V getLastValue() {
        Map.Entry<V, Object> lastEntry = map.getLastEntry();
        if (lastEntry == null) {
            return null;
        } else {
            return lastEntry.getKey();
        }
    }

    @Override
    public V pollLastValue() {
        Map.Entry<V, Object> lastEntry = map.pollLastEntry();
        if (lastEntry == null) {
            return null;
        } else {
            return lastEntry.getKey();
        }
    }

    @Override
    public boolean add(final V e) {
        return map.put(e, PRESENT) == null;
    }

    private static final Object PRESENT = new Object();

    @Override
    public boolean contains(final Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }





}
