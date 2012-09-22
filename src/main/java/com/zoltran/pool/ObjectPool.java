/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.pool;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    
    @Nonnull
    T borrowObject() throws ObjectCreationException;
    
    void returnObject(T object);
   
    void returnObject(T object, Exception e);
    
    
    public interface Hook<T> {
        
        void onBorrow(T object);
        
        void onReturn(T object);
        
    }
    
    public interface Factory<T> {
    
        T create() throws ObjectCreationException;
    
        void dispose(T object);
        
        boolean validate(T object, @Nullable Exception e);
        
    }
    
}
