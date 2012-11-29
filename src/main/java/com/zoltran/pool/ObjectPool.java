/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.pool;

import com.sun.java_cup.internal.runtime.Scanner;
import java.util.concurrent.TimeoutException;
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
public interface ObjectPool<T>  extends Disposable {
    
    /**
     * block until a object is available for the pool and return it.
     *
     * @return
     * @throws ObjectCreationException
     * @throws ObjectBorrowException
     * @throws InterruptedException
     * @throws TimeoutException 
     */
    
    @Nonnull
    T borrowObject() throws ObjectCreationException, ObjectBorrowException,
        InterruptedException, TimeoutException;
      
    
    /**
     * return a object previously borrowed from the pool, 
     * together with a optional exception in case one was encountered while using the object.
     * 
     * @param object
     * @param e
     * @throws ObjectReturnException
     * @throws ObjectDisposeException 
     */
    void returnObject(T object, @Nullable Exception e)  
            throws ObjectReturnException, ObjectDisposeException;
    
        
    public interface Hook<T> {
        
        void handle(T object) throws Exception;
        
    }
    
    public interface Factory<T> {
    
        /**
         * create the object.
         * @return
         * @throws ObjectCreationException 
         */
        T create() throws ObjectCreationException;
    
        
        /**
         * Dispose the object.
         * @param object
         * @throws ObjectDisposeException 
         */
        void dispose(T object) throws ObjectDisposeException;
        
        /**
         * Validate the object, return null if valid, a Exception with vallidation
         * detail otherwise.
         * 
         * @param object
         * @param e
         * @return 
         */ 
        @Nullable
        Exception validate(T object, @Nullable Exception e);
        
    }
    
}
