/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.vm;

import java.util.concurrent.ExecutorService;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class TestZelSingleThreaded {

    @Test
    public void testPi() throws CompileException, ZExecutionException, InterruptedException {
        String pi = "pi = func (x) {"
                + "term = func (k) {4 * (-1 ** k) / (2d * k + 1)};"
                + "for i = 0, result = 0; i < x; i = i + 1 { result = result + term(i) };"
                + "return result};"
                + "pi(x)";
        Program prog = Program.compile(pi, "x");
        long startTime = System.currentTimeMillis();
        Number result = (Number) prog.execute((ExecutorService) null, 100000);
        long endTime = System.currentTimeMillis();
        System.out.println("zel pi = " + result + " in " + (endTime - startTime) + "ms");
        // pi is 3.141592653589793
        Assert.assertEquals(3.141592653589793, result.doubleValue(), 0.0001);

    }

}
