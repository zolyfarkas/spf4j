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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class TestZelVsGroovy {

    @Test
    public void testZelVsGroovy()
            throws ExecutionException, ZExecutionException, InterruptedException, CompileException {
        testZelVsGroovy2();
        testZelVsGroovy2();
        testZelVsGroovy2();
    }
    
    public void testZelVsGroovy2()
            throws ExecutionException, ZExecutionException, InterruptedException, CompileException {
        java.lang.Number actualReturn = null;
        java.lang.Number actualReturn2 = null;
        Program p1 = Program.compile("a-b+1+c.length() - d.toString().substring(0, 1).length()", "a", "b", "c", "d");
        
        GroovyShell shell = new GroovyShell();
        Script gScript = shell.parse("a-b+1+c.length() - d.toString().substring(0, 1).length()");
        
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            actualReturn = (java.lang.Number) p1.execute(3, 2, " ", "bla");
        }
        long t2 = System.currentTimeMillis();

        for (int i = 0; i < 1000000; i++) {
            Binding binding = new Binding();
            binding.setVariable("a", Integer.valueOf(3));
            binding.setVariable("b", Integer.valueOf(2));
            binding.setVariable("c", " ");
            binding.setVariable("d", "bla");
            gScript.setBinding(binding);
            actualReturn2 = (Number) gScript.run();
        }
        long t3 = System.currentTimeMillis();

        System.out.println("precompiled via zel: " + (t2 - t1));
        System.out.println("precompiled via groovy: " + (t3 - t2));

        assertEquals(2, actualReturn.intValue());
        assertEquals(2, actualReturn2.intValue());
    }

}
