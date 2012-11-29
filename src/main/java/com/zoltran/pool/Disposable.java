/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.pool;

/**
 *
 * @author zoly
 */
public interface Disposable {
    
    public void dispose() throws ObjectDisposeException;
}
