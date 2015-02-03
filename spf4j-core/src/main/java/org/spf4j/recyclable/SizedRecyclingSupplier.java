
package org.spf4j.recyclable;

/**
 *
 * @author zoly
 */
public interface SizedRecyclingSupplier<T> {

    T get(int size);

    void recycle(T object);

    public interface Factory<T> {
        T create(int size);

        int size(T object);
    }
}
