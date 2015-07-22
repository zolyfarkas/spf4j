
package org.spf4j.base;

/**
 *
 * @author zoly
 */
public interface ParameterizedSupplier<W, PT> {

    W get(PT parameter);

}
