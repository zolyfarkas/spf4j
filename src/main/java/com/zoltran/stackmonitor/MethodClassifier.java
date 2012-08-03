/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.stackmonitor;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author zoly
 */
public final class MethodClassifier {

    private MethodClassifier() {}
    
    private static final Set<Method> WAIT_METHODS = new HashSet();

    static {
        WAIT_METHODS.add(new Method(sun.misc.Unsafe.class, "park"));
        WAIT_METHODS.add(new Method(java.lang.Object.class, "wait"));
        WAIT_METHODS.add(new Method(java.lang.Thread.class, "sleep"));
        WAIT_METHODS.add(new Method("java.net.PlainSocketImpl", "socketAccept"));
        WAIT_METHODS.add(new Method("java.net.PlainSocketImpl", "socketConnect"));
    }

    public static boolean isWaitMethod(Method m) {
        return WAIT_METHODS.contains(m);
    }
}
