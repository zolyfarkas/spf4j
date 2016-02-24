
package org.spf4j.base;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 *
 * @author zoly
 */
public enum ReferenceType {

    WEAK(new ReferenceFactory() {

        @Override
        public Reference<Object> create(final Object object) {
            return new WeakReference(object);
        }
    }),
    SOFT(new ReferenceFactory() {

        @Override
        public Reference<Object> create(final Object object) {
            return new SoftReference(object);
        }
    });


    private final ReferenceFactory factory;

    ReferenceType(final ReferenceFactory factory) {
        this.factory = factory;
    }

    public <T> Reference<T> create(final T object) {
        return factory.create(object);
    }

}
