/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.spf4j.zel.vm;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 *
 * @author zoly
 */
public class PushFuture<V> extends FutureTask<V>{

    public PushFuture(Callable<V> callable) {
        super(callable);
    }
    
    
    
}
