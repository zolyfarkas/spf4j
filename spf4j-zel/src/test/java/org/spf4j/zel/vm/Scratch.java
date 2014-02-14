/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.spf4j.zel.vm;

import org.junit.Test;

/**
 *
 * @author zoly
 */
public class Scratch {
    
    
    @Test
    public void test() throws CompileException, ZExecutionException, InterruptedException {
//        Program p1 = Program.compile("a-b+1+c.length()", "a", "b", "c");
//        Number actualReturn = (Number) p1.execute(3, 2, "");
//        System.out.println(actualReturn);
       String fib = "fib = func det (x) {fib(x-1) + fib(x-2)}; fib(0) = 0; fib(1) = 1; fib(x)";
        Program fibZel = Program.compile(fib, "x");
        long startTime = System.currentTimeMillis();
        Number zelResult = (Number) fibZel.execute(40);
        
    }
}
