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
        Program.compile("out(\"Hello World\")").execute();
    }

    @Test
    public void testBasicArithmetic()
            throws CompileException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("// simple expression \n 1+5*4/(1+1)");
        Number result = (Number) prog.execute();
        assertEquals(11, result.intValue());
    }

    @Test
    public void testJavaExec()
            throws CompileException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("a.toString().substring(0, 1 + 1)", "a");
        String result = (String) prog.execute(Integer.valueOf(100));
        assertEquals("100".substring(0, 2), result);
    }

    @Test
    public void testAssignement()
            throws CompileException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("a = a + 1", "a");
        Integer result = (Integer) prog.execute(100);
        assertEquals(101, result.intValue());
    }

    @Test
    public void testFib()
            throws CompileException, ZExecutionException, InterruptedException {
        String program
                = "fib = function deterministic (x) { fib(x-1) + fib(x-2) };\n"
                + "fib(0) = 0;\n"
                + "fib(1) = 1;\n"
                + "val1 = fib(100);\n"
                + "val2 = fib(200);\n"
                + "val3 = fib(1000);\n"
                + "val = val1 + val2;"
                + "out(val3);"
                + "val2";

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
        String fib = "fib = func det (x) {fib(x-1) + fib(x-2)}; fib(0) = 0; fib(1) = 1; fib(x)";
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

    @Test
    public void testParallelism() throws CompileException, ZExecutionException, InterruptedException {
        String program = "f1 = func {sleep 5000; 1}"
                + "f2 = func {sleep 5000; 2}"
                + "f1() + f2()";
        Program prog = Program.compile(program);
        System.out.println(prog);
        long startTime = System.currentTimeMillis();
        Number result = (Number) prog.execute();
        long endTime = System.currentTimeMillis();
        Assert.assertEquals(3, result.intValue());
        Assert.assertTrue("functions need to execute in parallel not in " + (endTime - startTime),
                endTime - startTime < 5200);
    }

    /**
     * GO equivalent:
     *
     * // Concurrent computation of pi.
     *
     * func main() { fmt.Println(pi(5000)) }
     *
     * // pi launches n goroutines to compute an // approximation of pi.
     * func pi(n int) float64 {
     * ch := make(chan float64)
     * for k := 0; k <= n; k++
     * { go term(ch, float64(k)) }
     * f := 0.0
     * for k := 0; k <= n; k++ { f += <-ch }
     * return f
     * }
     *
     * func term(ch chan float64, k float64) { ch <- 4 * math.Pow(-1, k) / (2*k + 1) }
     *
     *
     *
     * @throws CompileException
     * @throws ZExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testParallelPi() throws CompileException, ZExecutionException, InterruptedException {
        String pi = "pi = func (x) {"
                + "term = func (k) {4 * (-1 ** k) / (2d * k + 1)};"
                + "for i = 0; i < x; i = i + 1 { parts[i] = term(i) };"
                + "for result = 0, i = 0; i < x; i = i + 1 { result = result + parts[i] }"
                + "return result};"
                + "pi(x)";
        Program prog = Program.compile(pi, "x");
        System.out.println(prog);
        long startTime = System.currentTimeMillis();
        Number result = (Number) prog.execute(100000);
        long endTime = System.currentTimeMillis();
        System.out.println("zel pi = " + result + " in " + (endTime - startTime) + "ms");
        // pi is 3.141592653589793
        Assert.assertEquals(3.141592653589793, result.doubleValue(), 0.0001);
        
        startTime = System.currentTimeMillis();
        double dresult = pi(100000);
        endTime = System.currentTimeMillis();
        System.out.println("java pi = " + dresult + " in " + (endTime - startTime) + "ms");
        
        
    }
    
     private static  double term(final int k) {
        return 4 * Math.pow(-1, k) / (2d * k + 1);
     }
    
     private static double pi(final int nr) {
         double result = 0;
         for (int i = 0; i < nr; i++) {
             result += term(i);
         }
         return result;
     }
    
    

}
