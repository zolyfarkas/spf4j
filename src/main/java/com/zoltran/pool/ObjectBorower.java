/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.pool;

import java.io.Closeable;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public interface ObjectBorower<T> extends Disposable {
    
    @Nullable
    T requestReturnObject();
    
    @Nullable
    T returnObjectIfAvailable() throws InterruptedException;
    

}
