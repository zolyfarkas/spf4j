/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.spf4j.base;

import java.util.concurrent.Callable;

/**
 *
 * @author zoly
 */
public final class MemorizedCallable<V> implements Callable<V> {

    private volatile V value;
    
    private final Callable<V> callable;

    public MemorizedCallable(final Callable<V> callable) {
        this.callable = callable;
        value = null;
    }
    
    
    
    @Override
    public V call() throws Exception {
        V result = value;
        if (result == null) {
            synchronized (this) {
                result = value;
                if (result == null) {
                    result = callable.call();
                    value = result;
                }
            }
        }
        return result;
    }
    
    
}
