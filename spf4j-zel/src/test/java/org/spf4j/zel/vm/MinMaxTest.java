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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class MinMaxTest {

    @Test
    public void test() throws CompileException, ExecutionException, InterruptedException, IOException {
        String qsort = Resources.toString(Resources.getResource(MinMaxTest.class, "minmax.zel"),
                Charsets.US_ASCII);
        Program p = Program.compile(qsort);
        System.out.println(p);
        Integer max = (Integer) p.execute();
        Assert.assertEquals(8, (int) max);
    }


}
