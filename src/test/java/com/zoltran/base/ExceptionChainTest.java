/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.base;

import com.google.common.base.Throwables;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class ExceptionChainTest {
    

    /**
     * Test of chain method, of class ExceptionChain.
     */
    @Test
    public void testChain() {
        System.out.println("chain");
        Throwable t = new RuntimeException("", new SocketTimeoutException("Boo timeout"));
        Throwable newRootCause = new TimeoutException("Booo");
        Throwable result = ExceptionChain.chain(t, newRootCause);
        result.printStackTrace();
        Assert.assertEquals(newRootCause, Throwables.getRootCause(result));
        Assert.assertEquals(3, Throwables.getCausalChain(result).size());
        
    }
}
