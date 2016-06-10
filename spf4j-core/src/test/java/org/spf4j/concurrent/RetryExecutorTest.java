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
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Callables;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class RetryExecutorTest {


    @Test
    public void testSubmitCallable() throws InterruptedException, ExecutionException {
        System.out.println("submit");
        final LifoThreadPoolExecutorSQP lifoThreadPoolExecutorSQP = new LifoThreadPoolExecutorSQP(10, "test");
        RetryExecutor instance = new RetryExecutor(lifoThreadPoolExecutorSQP,
                (final Callable<Object> parameter) -> new Callables.DelayPredicate<Exception>() {

          @Override
          public int apply(final Exception value) {
            return 0;
          }
        }, null);
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
        instance.shutdown();
    }
}
