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
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final  class ChannelTest {


    @Test
    public void test() throws CompileException, ExecutionException, InterruptedException {

        String prog = "ch = channel();"
                    + "func prod(ch) { for i = 0; i < 100 ; i++ { ch.write(i) }; ch.close()}; "
                    + "func cons(ch, nr) "
                    + "{ sum = 0; "
                    + "for c = ch.read(); c != EOF; c = ch.read() {"
                    + " out(c, \",\"); sum++ };"
                    + " out(\"fin(\", nr, \",\", sum,\")\") };"
                    + "prod(ch)&; "
                    + "for i = 0; i < 10; i++ { cons(ch, i)& } ";

//        String prog =
//                     "func prod(ch) { out(\"A\"); for i = 0; i < 10 ; i++ { out(i); ch.write(i) }; out(\"B\")}; "
//                    + "func cons(ch) { out(ch.read()) };"
//                    + "prod(channel()) ";
//                    + "for i = 0; i < 10; i++ { cons(ch) } ";


        Program p = Program.compile(prog);
        System.out.println(p);
        p.execute();

    }
}
