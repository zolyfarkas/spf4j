/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool;

/**
 *
 * @author zoly
 */
public interface Scanable<T> {
    
    /**
     * 
     * @param handler
     * @return false if scan operation aborted by handler 
     */
    boolean scan( ScanHandler<T> handler );  
            
    interface ScanHandler<O> {
        
        /**
         * method to handle object
         * @param object
         * @return true if scan operation is to continue
         */        
        boolean handle(O object);
    }
    
}
