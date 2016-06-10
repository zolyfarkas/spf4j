
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.concurrent.DefaultScheduler;

/**
 *
 * @author zoly
 */
public final class RuntimeTest {

    /**
     * Test of goDownWithError method, of class Runtime.
     */
    @Test
    public void testSomeParams() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        System.out.println("PID=" + Runtime.PID);
        System.out.println("OSNAME=" + Runtime.OS_NAME);
        int nrOpenFiles = Runtime.getNrOpenFiles();
        System.out.println("NR_OPEN_FILES=" + nrOpenFiles);
        Assert.assertThat(nrOpenFiles, Matchers.greaterThan(0));
        String lsofOutput = Runtime.getLsofOutput();
        System.out.println("LSOF_OUT=" + lsofOutput);
        Assert.assertNotNull(lsofOutput);
        Assert.assertThat(lsofOutput, Matchers.containsString("jar"));
        System.out.println("MAX_OPEN_FILES=" + Runtime.Ulimit.MAX_NR_OPENFILES);
    }

    @Test(expected = ExecutionException.class, timeout = 60000)
    public void testExitCode() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Runtime.jrun(RuntimeTest.TestError.class, 60000);
    }

    @Test(expected = ExecutionException.class, timeout = 60000)
    public void testExitCode2() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Runtime.jrun(RuntimeTest.TestError2.class, 60000);
    }

    @Test(expected = TimeoutException.class, timeout = 30000)
    public void testExitCode3() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Runtime.jrun(RuntimeTest.TestError3.class, 10000);
    }


    @Test(expected = InterruptedException.class, timeout = 30000)
    public void testExitCode4() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final Thread t = Thread.currentThread();
        DefaultScheduler.INSTANCE.schedule(() -> { t.interrupt(); }, 1, TimeUnit.SECONDS);
        Runtime.jrun(RuntimeTest.TestError3.class, 10000);
    }

    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    @Test(expected = CancellationException.class, timeout = 30000)
    public void testExitCode5() throws  InterruptedException, ExecutionException, TimeoutException {
        final CountDownLatch latch  = new CountDownLatch(1);
        final CountDownLatch canCancel  = new CountDownLatch(1);
        Future<?> submit = DefaultExecutor.INSTANCE.submit(new AbstractRunnable() {

            @Override
            public void doRun() throws IOException, InterruptedException, ExecutionException, TimeoutException {
                try {
                    canCancel.countDown();
                    Runtime.jrun(RuntimeTest.TestError3.class, 10000);
                } catch (InterruptedException ex) {
                    Throwables.writeTo(ex, System.err, Throwables.Detail.STANDARD);
                    latch.countDown();
                } catch (Exception ex) {
                  Throwables.writeTo(ex, System.err, Throwables.Detail.STANDARD);
                }
            }
        });
        if (!canCancel.await(3000, TimeUnit.MILLISECONDS)) {
          Assert.fail("exec should happen");
        }
        submit.cancel(true);
        if (!latch.await(15000, TimeUnit.SECONDS)) {
            Assert.fail("exec should be cancelled");
        }
        submit.get(10000, TimeUnit.MILLISECONDS);

    }



    public static final class TestError {

        public static void main(final String [] args) {
            throw new RuntimeException();
        }
    }

    public static final class TestError2 {

        public static void main(final String [] args) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(final Thread t, final Throwable e) {
                    Throwables.writeTo(e, System.err, Throwables.Detail.STANDARD);
                }
            });
            throw new RuntimeException();
        }
    }

    public static final class TestError3 {

        public static void main(final String [] args) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(final Thread t, final Throwable e) {
                    Throwables.writeTo(e, System.err, Throwables.Detail.STANDARD);
                }
            });
            DefaultScheduler.INSTANCE.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                }
            }, 10, 10, TimeUnit.MILLISECONDS);

            throw new RuntimeException();
        }
    }




}