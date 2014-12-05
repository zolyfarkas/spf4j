
package org.spf4j.recyclable;

import com.google.common.base.Supplier;

/**
 *
 * @author zoly
 */
public interface Lease<T> extends Supplier<T>, AutoCloseable {
    
}