
package org.spf4j.recyclable;

/**
 *
 * @author zoly
 */
public interface SizedLeaseSupplier<T>  {

        Lease<T> lease(int size);

}
