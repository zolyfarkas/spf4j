/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
    public void helloWorld()
            throws CompileException, ZExecutionException, InterruptedException {
        Program.compile("out(\"Hello World\");").execute();
    }
    
    
    @Test
    public void testBasicArithmetic()
            throws CompileException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("// simple expression \n 1+5*4/(1+1);");
        Number result = (Number) prog.execute();
        assertEquals(11, result.intValue());
    }

    @Test
    public void testJavaExec()
            throws CompileException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("a.toString().substring(0, 1 + 1);", "a");
        String result = (String) prog.execute(Integer.valueOf(100));
        assertEquals("100".substring(0, 2), result);
    }

    @Test
    public void testAssignement()
            throws CompileException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("a = a + 1;", "a");
        Integer result = (Integer) prog.execute(100);
        assertEquals(101, result.intValue());
    }

    @Test
    public void testFib()
            throws CompileException, ZExecutionException, InterruptedException {
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

    public BigInteger fibB(final int i) {
        if (i <= 1) {
            return BigInteger.valueOf(i);
        } else {
            return fibB(i - 1).add(fibB(i - 2));
        }
    }

    public long fib(final long i) {
        if (i <= 1) {
            return i;
        } else {
            return fib(i - 1) + fib(i - 2);
        }
    }

    public BigInteger fibBNr(final int i) {
        if (i <= 1) {
            return BigInteger.valueOf(i);
        } else {
            BigInteger v1 = BigInteger.ZERO;
            BigInteger v2 = BigInteger.ONE;
            BigInteger tmp;
            for (int j = 2; j <= i; j++) {
                tmp = v2;
                v2 = v1.add(v2);
                v1 = tmp;
            }
            return v2;
        }
    }
        

    @Test
    public void testFibPerformance() throws CompileException, ZExecutionException, InterruptedException {
        String fib = "fib = func det (x) {fib(x-1) + fib(x-2);}; fib(0) = 0; fib(1) = 1; fib(x);";
        Program fibZel = Program.compile(fib, "x");
        System.out.println(fibZel);
        long startTime = System.currentTimeMillis();
        Number zelResult = (Number) fibZel.execute(40);
        long intTime = System.currentTimeMillis();
        long javaResult = fib(40);
        long stopTime = System.currentTimeMillis();
        Assert.assertEquals(zelResult.longValue(), javaResult);
        System.out.println("zel exec " + (intTime - startTime));
        System.out.println("java exec " + (stopTime - intTime));

        startTime = System.currentTimeMillis();
        BigInteger zelResult2 = (BigInteger) fibZel.execute(10000);
        intTime = System.currentTimeMillis();
        BigInteger javaResult2 = fibBNr(10000);
        stopTime = System.currentTimeMillis();
        System.out.println("fib(10000) = " + zelResult2);
        System.out.println("fib(10000) = " + javaResult2);
        Assert.assertEquals(zelResult2, javaResult2);
        System.out.println("zel exec " + (intTime - startTime));
        System.out.println("java exec " + (stopTime - intTime));
    }

}
