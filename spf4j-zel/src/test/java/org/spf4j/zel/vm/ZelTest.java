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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("HES_LOCAL_EXECUTOR_SERVICE")
public final class ZelTest {

    @Test
    public void helloWorld()
            throws CompileException, ExecutionException, InterruptedException, UnsupportedEncodingException {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Program.compile("out(\"Hello World\")").execute(System.in,
                new PrintStream(bos, true, StandardCharsets.UTF_8.toString()), System.err);
      Assert.assertEquals("Hello World", new String(bos.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testBasicArithmetic()
            throws CompileException, ExecutionException, InterruptedException {
        Program prog = Program.compile("// simple expression \n 1+5*4/(1+1)");
        Number result = (Number) prog.execute();
        assertEquals(11, result.intValue());
    }

    @Test
    public void testJavaExec()
            throws CompileException, ExecutionException, InterruptedException {
        Program prog = Program.compile("a.toString().substring(0, 1 + 1)", "a");
        String result = (String) prog.execute(Integer.valueOf(100));
        assertEquals("100".substring(0, 2), result);
    }

    @Test
    public void testJavaExecStatic()
            throws CompileException, ExecutionException, InterruptedException {
        Program prog = Program.compile("s.format(\"Number %d\", 3)", "s");
        String result = (String) prog.execute(String.class);
        assertEquals(String.format("Number %d", 3), result);
    }

    @Test
    public void testAssignement()
            throws CompileException, ExecutionException, InterruptedException {
        Program prog = Program.compile("a = a + 1", "a");
        Integer result = (Integer) prog.execute(100);
        assertEquals(101, result.intValue());
    }

    @Test
    public void testFib()
            throws CompileException, ExecutionException, InterruptedException {
        String program
                = "function deterministic fib (x) { fib(x-1) + fib(x-2) };\n"
                + "fib(0) = 0;\n"
                + "fib(1) = 1;\n"
                + "val1 = fib(100);\n"
                + "val2 = fib(200);\n"
                + "val3 = fib(1000);\n"
                + "val = val1 + val2;"
                + "out(val3);"
                + "val2";

        Program compiledProgram = Program.compile(program);
        System.out.println(compiledProgram);
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
    public void testFibPerformance() throws CompileException, ExecutionException, InterruptedException {
        String fib = "func det fib (x) {fib(x-1) + fib(x-2)}; fib(0) = 0; fib(1) = 1; fib(x)";
        Program fibZel = Program.compile(fib, "x");
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
    public void testParallelism() throws CompileException, ExecutionException, InterruptedException {
        String program = "f1 = func {sleep 5000; 1};"
                + "f2 = func {sleep 5000; 2};"
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

        @Test
    public void testParallelism2() throws CompileException, ExecutionException, InterruptedException {
        String program = "f1 = func {sleep 5000; 1};"
                + "f2 = func {sleep 5000; 2};"
                + "f1() + f2()";
        Program prog = Program.compile(program);
        System.out.println(prog);
        long startTime = System.currentTimeMillis();
        Number result = (Number) prog.execute(Executors.newSingleThreadExecutor());
        long endTime = System.currentTimeMillis();
        Assert.assertEquals(3, result.intValue());
        Assert.assertTrue("functions need to execute in parallel not in " + (endTime - startTime),
                endTime - startTime < 5200);
    }


    @Test
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public void testCond() throws CompileException, ExecutionException, InterruptedException {
        Boolean result = (Boolean) Program.compile("x == 1", "x").execute(1);
        Assert.assertTrue(result);
        result = (Boolean) Program.compile("x != 1", "x").execute(1);
        Assert.assertFalse(result);
        result = (Boolean) Program.compile("x < 1", "x").execute(1);
        Assert.assertFalse(result);
        result = (Boolean) Program.compile("x >= 1", "x").execute(1);
        Assert.assertTrue(result);
        result = (Boolean) Program.compile("x <= 1", "x").execute(1);
        Assert.assertTrue(result);
        result = (Boolean) Program.compile("x > 1", "x").execute(1);
        Assert.assertFalse(result);
        Number n = (Number) Program.compile("min(3, 1, 8)").execute();
        Assert.assertEquals(1, n.intValue());
        n = (Number) Program.compile("max(3, 1, 8, 10)").execute();
        Assert.assertEquals(10, n.intValue());
        n = (Number) Program.compile("sqrt(4)").execute();
        Assert.assertEquals(2, n.intValue());
        n = (Number) Program.compile("log(E)").execute();
        Assert.assertEquals(1, n.intValue());

    }

    @Test
    public void testAsync() throws CompileException, ExecutionException, InterruptedException {
        String prog = "f = func (a, b) {sleep 1000; a + b };"
                + "f(f(1, 2),f(3, 4))";
        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(8);
        long startTime = System.currentTimeMillis();
        Number result = (Number) Program.compile(prog).execute(newFixedThreadPool);
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println(elapsed);
 //       Assert.assertTrue("Execution is " + elapsed + "should be smaller than 1200" , elapsed < 1200);
        Assert.assertEquals(10, result.intValue());
        newFixedThreadPool.shutdown();
    }

    @SuppressFBWarnings("MDM_THREAD_YIELD")
    private static class TestF {
        public static int f(final int a, final int b) throws InterruptedException {
            Thread.sleep(1000);
            return a + b;
        }
    }

    @Test
    public void testAsync2() throws CompileException, ExecutionException, InterruptedException {
        ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(8);
        String prog = "f(f(1, 2)&,f(3, 4)&)&";
        long startTime = System.currentTimeMillis();
        Number result = (Number) Program.compile(prog, "f").execute(newFixedThreadPool, new JavaMethodCall(TestF.class, "f"));
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println(elapsed);
//        Assert.assertTrue(elapsed < 2200);
        Assert.assertEquals(10, result.intValue());
        newFixedThreadPool.shutdown();
    }


    @Test
    public void testArrays() throws CompileException, ExecutionException, InterruptedException {
       String result = (String) Program.compile("x.split(\",\")[1]", "x").execute("a,b,c");
       Assert.assertEquals("b", result);
       result = (String) Program.compile("x.split(\",\")[1] = \"A\"", "x").execute("a,b,c");
       Assert.assertEquals("A", result);
    }

    @Test
    public void testCond2() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("x >= 0 ? \"positive\" : \"negative\" ", "x");
       System.out.println(p);
       String result = (String) p.execute(1);
       Assert.assertEquals("positive", result);
    }

    @Test
    public void testCond3() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile(" if x >= 0 { \"positive\" } else { \"negative\" } ", "x");
       System.out.println(p);
       String result = (String) p.execute(1);
       Assert.assertEquals("positive", result);
    }

    @Test
    public void testFor() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("x = 0; for i=0; i < 10; i++ {out(i); x = x + i}; x ");
       System.out.println(p);
       Integer result = (Integer) p.execute(1);
       Assert.assertEquals(45, result.intValue());
    }

    @Test
    @SuppressFBWarnings("SACM_STATIC_ARRAY_CREATED_IN_METHOD")
    public void testSwap() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("x[0] <-> x[1]; x[1]", "x");
       System.out.println(p);
       String [] testArray = new String[] {"a", "b"};
       String result = (String) p.execute(new Object [] {testArray});
       Assert.assertEquals("a", result);
    }

    @Test
    public void testArray() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("x = array(2); x.length");
       System.out.println(p);
       Integer result = (Integer) p.execute();
       Assert.assertEquals(2, result.intValue());
    }


    @Test
    public void testMultiRet() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("func test {"
               + "ret {1, 2} };"
               + "x,y = test();"
               + "out(y); ret x");
       System.out.println(p);
       Integer result = (Integer) p.execute();
       Assert.assertEquals(1, result.intValue());
    }


    @Test
    public void testMultiAssignement() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("x, y, z = {1, 2, 3}; ret y");
       System.out.println(p);
       Integer result = (Integer) p.execute();
       Assert.assertEquals(2, result.intValue());
    }


    @Test
    public void testAbs() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("|3 - 5|");
       System.out.println(p);
       Integer result = (Integer) p.execute();
       Assert.assertEquals(2, result.intValue());
    }

    @Test
    public void testDecode() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("decode(1+1, 3, 0, 1, -1, 2, 666, 777)");
       System.out.println(p);
       Integer result = (Integer) p.execute();
       Assert.assertEquals(666, result.intValue());
    }

    @Test
    public void testDecode2() throws CompileException, ExecutionException, InterruptedException {
       Program p = Program.compile("decode(1+1, 3, 0, 1, -1, 100, 666, 777)");
       System.out.println(p);
       Integer result = (Integer) p.execute();
       Assert.assertEquals(777, result.intValue());
    }



}
