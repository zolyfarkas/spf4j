
package org.spf4j.recyclable;

import java.util.function.Supplier;


/**
 *
 * @author zoly
 */
public interface Lease<T> extends Supplier<T>, AutoCloseable {

}