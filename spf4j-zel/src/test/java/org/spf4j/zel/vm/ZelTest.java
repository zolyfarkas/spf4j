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
    public void testBasicArithmetic()
            throws CompileException, ZExecutionException, InterruptedException {
        Program prog = Program.compile("1+5*4/(1+1);");
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

}
