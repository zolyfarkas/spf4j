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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 *
 * @author zoly
 */
public final class TestZelVsSpel {

    @Test
    public void testZelVsSpel()
            throws ExecutionException, ZExecutionException, InterruptedException, CompileException {
        java.lang.Number actualReturn = null;
        java.lang.Number actualReturn2 = null;
        Program p1 = Program.compile("a-b", "a", "b");
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression("['a']-['b']");

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            actualReturn = (java.lang.Number) p1.execute(3, 2);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("precompiled via zel: " + (t2 - t1));
       
        for (int i = 0; i < 100; i++) {
            Map vars = new HashMap();
            vars.put("a", Integer.valueOf(3));
            vars.put("b", Integer.valueOf(2));
            actualReturn2 = (Number) exp.getValue(vars, Integer.class);
        }
        long t3 = System.currentTimeMillis();

        System.out.println("precompiled via spel: " + (t3 - t2));

        assertEquals(1, actualReturn.intValue());
        assertEquals(1, actualReturn2.intValue());
    }

}
