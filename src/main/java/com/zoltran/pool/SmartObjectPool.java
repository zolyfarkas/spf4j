/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.pool;

import java.util.concurrent.TimeoutException;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public interface SmartObjectPool<T> extends Disposable, Scanable<T> {
    
    T borrowObject(ObjectBorower borower) throws InterruptedException,
            TimeoutException, ObjectCreationException;
    
    void returnObject(T object, ObjectBorower borower);
    
}
