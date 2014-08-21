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

import com.google.common.base.Predicate;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Callables.TimeoutCallable;

/**
 *
 * @author zoly
 */
public final class CallablesTest {


    /**
     * Test of executeWithRetry method, of class Callables.
     */
    @Test
    public void testExecuteWithRetry4args1() throws Exception {
        System.out.println("executeWithRetry");
        Integer result = Callables.executeWithRetry(new TimeoutCallable<Integer>(60000) {
            @Override
            public Integer call(final long deadline) throws Exception {
                return 1;
            }
        }, 3, 10);
        Assert.assertEquals(1L, result.longValue());
    }

    /**
     * Test of executeWithRetry method, of class Callables.
     */
    @Test
    public void testExecuteWithRetry4args2() throws Exception {
        System.out.println("executeWithRetry");
        long startTime = System.currentTimeMillis();
        Integer result = Callables.executeWithRetry(new TimeoutCallable<Integer>(60000) {
            private int count;

            @Override
            public Integer call(final long deadline) throws Exception {
                count++;
                if (count < 5) {
                    throw new IOException("Aaaaaaaaaaa" + count);
                }

                return 1;
            }
        }, 1, 10);
        long elapsedTime = System.currentTimeMillis() - startTime;
        Assert.assertEquals(1L, result.longValue());
        Assert.assertTrue("Operation has to take at least 10 ms", elapsedTime > 10L);
    }
    
    @Test
    public void testExecuteWithRetryFailureTest() throws Exception {
        System.out.println("executeWithRetry");
        try {
            Integer result = Callables.executeWithRetry(new TimeoutCallable<Integer>(10) {
                private int count;

                @Override
                public Integer call(final long deadline) throws Exception {
                    count++;
                    if (count < 200) {
                        throw new IOException("Aaaaaaaaaaa " + System.currentTimeMillis());
                    }
                    return 1;
                }
            }, 4, 100);
            Assert.fail("Should not get here");
        } catch (Exception e) {
            if (Runtime.JAVA_PLATFORM == Runtime.Version.V1_6) {
                Assert.assertEquals(5, Throwables.getSuppressed(e.getCause()).length);
            } else {
                Assert.assertEquals(1, Throwables.getSuppressed(e.getCause()).length);
            }
        }

    }

    
    @Test
    public void testSuppression() throws Exception {
        System.out.println("executeWithRetry");
        long startTime = System.currentTimeMillis();
        Integer result = Callables.executeWithRetry(new TimeoutCallable<Integer>(60000) {
            private int count;

            @Override
            public Integer call(final long deadline) throws Exception {
                count++;
                if (count < 10) {
                    throw new IOException("Aaaaaaaaaaa" + count);
                }

                return 1;
            }
        }, 1, 10, new Predicate<Exception>() {

            @Override
            public boolean apply(final Exception input) {
                final Throwable[] suppressed = Throwables.getSuppressed(input);
                if (suppressed.length > 0) {
                    throw new RuntimeException();
                }
                return true;
            }
        });
        long elapsedTime = System.currentTimeMillis() - startTime;
        Assert.assertEquals(1L, result.longValue());
        Assert.assertTrue("Operation has to take at least 10 ms", elapsedTime > 10L);
    }
    
    
    @Test(expected = RuntimeException.class)
    public void testExecuteWithRetryTimeout() throws Exception {
        System.out.println("executeWithRetryTimeout");
        Integer result = Callables.executeWithRetry(new TimeoutCallable<Integer>(1000) {
            private int count;

            @Override
            public Integer call(final long deadline) throws Exception {
                Thread.sleep(2000);
                count++;
                if (count < 5) {
                    throw new IOException("Aaaaaaaaaaa" + count);
                }
                return 1;
            }
        }, 1, 10);
        Assert.assertEquals(1L, result.longValue());
    }
    
    @Test(expected = RuntimeException.class)
    public void testExecuteWithRetryTimeout2() throws Exception {
        System.out.println("executeWithRetryTimeout2");
        Integer result = Callables.executeWithRetry(new TimeoutCallable<Integer>(1000) {
            private int count;

            @Override
            public Integer call(final long deadline) throws Exception {
                System.out.println("Exec at " + System.currentTimeMillis());
                count++;
                if (count < 200) {
                    throw new IOException("Aaaaaaaaaaa" + count);
                }
                return 1;
            }
        }, 0, 100);
        Assert.assertEquals(1L, result.longValue());
    }
    
    
    /**
     * Test of executeWithRetry method, of class Callables.
     */
    public void testExecuteWithRetry4args3() throws Exception {
        System.out.println("executeWithRetry");
        final CallableImpl callableImpl = new CallableImpl(60000);
        try {
            Callables.executeWithRetry(callableImpl, 3, 10);
            Assert.fail("this should throw a exception");
        } catch (Exception e) {
            Assert.assertEquals(11, callableImpl.getCount());
            System.out.println("Exception as expected " + e);
        }
    }
    
    
    public void testExecuteWithRetry5args3() throws Exception {
        System.out.println("executeWithRetry");
        final CallableImpl2 callableImpl = new CallableImpl2(60000);
        Callables.executeWithRetry(callableImpl, 2, 10, new Predicate<Integer>() {

            @Override
            public boolean apply(final Integer t) {
                return t > 0;
            }
        },
                Callables.DEFAULT_EXCEPTION_RETRY);
        Assert.assertEquals(4, callableImpl.getCount());
    }

    private static class CallableImpl extends TimeoutCallable<Integer> {


        private int count;

        public CallableImpl(final int timeoutMillis) {
            super(timeoutMillis);
        }
        
        @Override
        public Integer call(final long deadline) throws Exception {
            count++;
            throw new Exception("Aaaaaaaaaaa" + count);
        }

        public int getCount() {
            return count;
        }
        
    }
    
    private static class CallableImpl2 extends TimeoutCallable<Integer> {

        
        private int count;

        public CallableImpl2(final int timeoutMillis) {
            super(timeoutMillis);
        }
        
        @Override
        public Integer call(final long deadline) throws Exception {
            count++;
            return count;
        }

        public int getCount() {
            return count;
        }
        
    }
    
    
}
