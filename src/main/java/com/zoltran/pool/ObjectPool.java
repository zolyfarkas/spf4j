/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.pool;

import java.io.Closeable;

/**
 * Object pool interface.
 * 
 * My goal is to create a simpler and better object pool interface and implementation.
 * 
 * 
 * @author zoly
 */
public interface ObjectPool<T>  extends Closeable {
    
    T borrowObject();
    
    void returnObject(T object);
   
    void returnObject(T object, Exception e);
        
}
