
package org.spf4j.base;

import java.lang.ref.Reference;

/**
 *
 * @author zoly
 */
public interface ReferenceFactory<T> {
    Reference<T> create(T object);
}
