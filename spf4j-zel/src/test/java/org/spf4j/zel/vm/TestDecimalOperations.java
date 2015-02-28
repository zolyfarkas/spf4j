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

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final  class TestDecimalOperations {


    @Test
    public void test() throws CompileException, ExecutionException, InterruptedException {
        Program prog = Program.compile("use dec 64; 1.0/3");
        BigDecimal result = (BigDecimal) prog.execute();
        System.out.println(result.toPlainString());
        Assert.assertTrue(result.toPlainString().length() == 18);
    }

    @Test
    public void test2() throws CompileException, ExecutionException, InterruptedException {
        Program prog = Program.compile("use dec 128; 1.0/3");
        BigDecimal result = (BigDecimal) prog.execute();
        System.out.println(result.toPlainString());
        Assert.assertTrue(result.toPlainString().length() == 36);
    }

    @Test
    public void test3() throws CompileException, ExecutionException, InterruptedException {
        BigDecimal result = (BigDecimal) Program.compile("use dec 128; (1.0/3) * 3 + (1 - 1.0/3*3)").execute();
        System.out.println(result.toPlainString());
        Assert.assertTrue(BigDecimal.ONE.compareTo(result) == 0);
    }


}
