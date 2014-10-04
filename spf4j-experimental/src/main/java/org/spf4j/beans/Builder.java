
package org.spf4j.beans;


/**
 *
 * @author zoly
 */
public interface Builder<OF> {
    
    
    
    <T> Builder set(T getterCall, T value);
    
    OF of();
    
    OF build();
}
