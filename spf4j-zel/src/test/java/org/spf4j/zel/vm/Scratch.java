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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final  class Scratch {


    @Test
    public void test() throws CompileException, ExecutionException, InterruptedException {
//        Program p1 = Program.compile("a-b+1+c.length()", "a", "b", "c");
//        Number actualReturn = (Number) p1.execute(3, 2, "");
//        System.out.println(actualReturn);
//        Program prog = Program.compile("s.format(\"Number %d\", 3)", "s");
//        String result = (String) prog.execute(String.class);
//        assertEquals(String.format("Number %d", 3), result);


//                String prog = "f = func (a, b) {sleep 1000; a + b };"
//                + "f(f(1, 2),f(3, 4))";
//        long startTime = System.currentTimeMillis();
//        Number result = (Number) Program.compile(prog).execute();
//        long elapsed = System.currentTimeMillis() - startTime;
//        System.out.println(elapsed);
//        Assert.assertTrue("Execution is " + elapsed + "should be smaller than 1200" , elapsed < 1200);
//        Assert.assertEquals(10, result.intValue());
//

//                String program
//                = "func det fib (x) { fib(x-1) + fib(x-2) };\n"
//                + "fib(0) = 0;\n"
//                + "fib(1) = 1;\n"
//                + "fib(10)";
//
//        Program compiledProgram = Program.compile(program);
//        System.out.println(compiledProgram);
//        Number result = (Number) compiledProgram.execute();
//        System.out.println(result);
//

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


//       Program program = Program.compile("x.split(\",\")[1] = \"A\"", "x");
//       System.out.println(program);
//       String result = (String) program.execute("a,b,c");
//       Assert.assertEquals("A", result);

    }
}
