/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool;

/**
 *
 * @author zoly
 */
public class PoolException  extends Exception {

    public PoolException(Throwable cause) {
        super(cause);
    }

    public PoolException(String message, Throwable cause) {
        super(message, cause);
    }

    public PoolException(String message) {
        super(message);
    }

    public PoolException() {
    }
    
    
    
}
