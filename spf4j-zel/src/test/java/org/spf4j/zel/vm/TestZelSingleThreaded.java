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
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class TestZelSingleThreaded {

    @Test
    public void testPi() throws CompileException, ExecutionException, InterruptedException {
        for (int i = 0; i < 3; i++) {
            testPiImpl();
        }
    }


    public void testPiImpl() throws CompileException, ExecutionException, InterruptedException {
        String pi = "pi = func sync (x) {"
                + "term = func sync (k) {4 * (-1 ** k) / (2d * k + 1)};"
                + "for i = 0, result = 0; i < x; i = i + 1 { result = result + term(i) };"
                + "return result};"
                + "pi(x)";
        Program prog = Program.compile(pi, "x");
        long startTime = System.currentTimeMillis();
        Number result = (Number) prog.execute(100000);
        long endTime = System.currentTimeMillis();
        System.out.println("zel pi = " + result + " in " + (endTime - startTime) + "ms");
        // pi is 3.141592653589793
        Assert.assertEquals(3.141592653589793, result.doubleValue(), 0.0001);

    }

}
