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
    
    void scan( ScanHandler<T> handler );  
            
    interface ScanHandler<O> {
        boolean handle(O object);
    }
    
}
