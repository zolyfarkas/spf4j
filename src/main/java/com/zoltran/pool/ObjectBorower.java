/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.pool;

import java.util.Collection;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public interface ObjectBorower<T> extends Scanable<T>
{
    
    @Nullable
    T requestReturnObject();
    
    @Nullable
    T returnObjectIfAvailable() throws InterruptedException;
    
    @Nullable
    Collection<T> returnObjectsIfNotNeeded();
    
}
