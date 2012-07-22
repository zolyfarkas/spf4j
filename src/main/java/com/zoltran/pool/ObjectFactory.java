/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool;

/**
 *
 * @author zoly
 */
public interface ObjectFactory<T> {
    
    T create();
    
    void dispose(T object);
}
