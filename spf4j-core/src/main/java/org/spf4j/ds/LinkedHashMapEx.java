
package org.spf4j.ds;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public final class LinkedHashMapEx<K, V> extends LinkedHashMap<K, V>
    implements LinkedMap<K, V> {

    private static final Field HEADER;
    private static final Field HEADER_BEFORE;
    private static final long serialVersionUID = 1L;

    static {
        HEADER = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                try {
                    Field field = LinkedHashMap.class.getDeclaredField("header");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException | SecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        HEADER_BEFORE = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                try {
                    Class<?> entryClass = Class.forName("java.util.LinkedHashMap$Entry");
                    Field field = entryClass.getDeclaredField("before");
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException | SecurityException | ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

    }

    private Map.Entry<K, V> getHeader() {
        try {
            return (Map.Entry<K, V>) HEADER.get(this);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public LinkedHashMapEx() {
        super();
        header = getHeader();
    }


    public LinkedHashMapEx(final int initialCapacity, final float loadFactor) {
        super(initialCapacity, loadFactor);
        header = getHeader();
    }

    public LinkedHashMapEx(final int initialCapacity) {
        super(initialCapacity);
        header = getHeader();
    }



    public LinkedHashMapEx(final Map<? extends K, ? extends V> m) {
        super(m);
        header = getHeader();
    }

    public LinkedHashMapEx(final int initialCapacity, final float loadFactor, final boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
        header = getHeader();
    }


   private transient Map.Entry<K, V> header;

   private void readObject(final java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        header = getHeader();
   }


    private Map.Entry<K, V> getLastEntryInternal() {
        try {
            return (Map.Entry<K, V>) HEADER_BEFORE.get(header);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }




   @Nullable
    @Override
    public Map.Entry<K, V> getLastEntry() {
        final Entry<K, V> lastEntry = getLastEntryInternal();
        if (lastEntry == header) {
            return null;
        } else {
            return lastEntry;
        }
    }

    @Nullable
    @Override
    public Map.Entry<K, V> pollLastEntry() {
        final Entry<K, V> lastEntry = getLastEntryInternal();
        if (lastEntry == header) {
            return null;
        } else {
            remove(lastEntry.getKey());
            return lastEntry;
        }
    }


}
