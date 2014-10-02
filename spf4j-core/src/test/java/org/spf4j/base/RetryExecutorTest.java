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

package org.spf4j.base;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import junit.framework.Assert;
import org.junit.Test;
import org.spf4j.concurrent.RetryExecutor;

/**
 *
 * @author zoly
 */
public final class RetryExecutorTest {
    
    public RetryExecutorTest() {
    }

    @Test
    public void testSubmitCallable() throws InterruptedException, ExecutionException {
        System.out.println("submit");
        RetryExecutor instance = new RetryExecutor(Executors.newFixedThreadPool(10), 3,
                10, 100, Callables.DEFAULT_EXCEPTION_RETRY, null);
        Future result = instance.submit(new Callable<Integer>() {

            private int count;
            @Override
            public Integer call() throws Exception {
                System.out.println("exec " + count + " st " + System.currentTimeMillis());
                count++;
                if (count < 5) {
                    throw new IOException("Aaaaaaaaaaa" + count);
                }
                    
                return 1;
            }
        });
        Assert.assertEquals(1, result.get());
    }
}
