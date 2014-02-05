/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zel.vm;

import java.math.BigInteger;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


/**
 *
 * @author zoly
 */
public final class ZelTest {

    @Test
    public void testBasicArithmetic()
            throws ParseException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("1+5*4/(1+1);");
        Number result = (Number) prog.execute();
        assertEquals(11, result.intValue());
    }
    
    
    @Test
    public void testJavaExec()
            throws ParseException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("a.toString().substring(0, 1 + 1);", "a");
        String result = (String) prog.execute(Integer.valueOf(100));
        assertEquals("100".substring(0, 2), result);
    }

    @Test
    public void testAssignement()
            throws ParseException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("a = a + 1;", "a");
        Integer result = (Integer) prog.execute(Integer.valueOf(100));
        assertEquals(101, result.intValue());
    }

    @Test
    public void testFib()
            throws ParseException, ZExecutionException, InterruptedException {
        String program
                = "fib = function deterministic (x) { fib(x-1) + fib(x-2); };\n"
                + "fib(0) = 0;\n"
                + "fib(1) = 1;\n"
                + "val1 = fib(100);\n"
                + "val2 = fib(200);\n"
                + "val3 = fib(1000);\n"
                + "val = val1 + val2;"
                + "out(val3);"
                + "val2;";

        Program compiledProgram = Program.compile(program);
        Number result = (Number) compiledProgram.execute();
        Assert.assertEquals(new BigInteger("280571172992510140037611932413038677189525"), result);
        System.out.println(result);
    }

}
