/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.stackmonitor;

import com.google.common.base.Predicate;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author zoly
 */
public final class WaitMethodClassifier implements Predicate<Method> {

    private WaitMethodClassifier() {}
    
    private static final Set<Method> WAIT_METHODS = new HashSet();

    static {
        WAIT_METHODS.add(new Method(sun.misc.Unsafe.class, "park"));
        WAIT_METHODS.add(new Method(java.lang.Object.class, "wait"));
        WAIT_METHODS.add(new Method(java.lang.Thread.class, "sleep"));
        WAIT_METHODS.add(new Method("java.net.PlainSocketImpl", "socketAccept"));
        WAIT_METHODS.add(new Method("java.net.PlainSocketImpl", "socketConnect"));
    }

   
    @Override
    public boolean apply(Method input) {
       return WAIT_METHODS.contains(input); 
    }
    
    public static final WaitMethodClassifier INSTANCE = new WaitMethodClassifier();
}
