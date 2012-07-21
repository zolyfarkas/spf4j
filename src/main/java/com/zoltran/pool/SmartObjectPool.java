/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.pool;

import java.io.Closeable;

/**
 *
 * @author zoly
 */
public interface SmartObjectPool<T> extends Closeable {
    
    T borrowObject(ObjectBorower borower);
    
    void returnObject(T object, ObjectBorower borower);
    
}
