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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final  class Scratch {
    
    
    @Test
    public void test() throws CompileException, ZExecutionException, InterruptedException {
//        Program p1 = Program.compile("a-b+1+c.length()", "a", "b", "c");
//        Number actualReturn = (Number) p1.execute(3, 2, "");
//        System.out.println(actualReturn);
        Program prog = Program.compile("s.format(\"Number %d\", 3)", "s");
        String result = (String) prog.execute(String.class);
        assertEquals(String.format("Number %d", 3), result);
        
    }
}
