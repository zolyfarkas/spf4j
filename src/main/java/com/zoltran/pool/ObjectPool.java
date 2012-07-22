/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.pool;

import java.io.Closeable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Object pool interface.
 * 
 * My goal is to create a simpler and better object pool interface and implementation.
 * 
 * 
 * @author zoly
 */
@ParametersAreNonnullByDefault
public interface ObjectPool<T>  extends Disposable, Scanable<T> {
    
    T borrowObject();
    
    void returnObject(T object);
   
    void returnObject(T object, Exception e);
    
}
