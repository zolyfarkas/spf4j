/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.base;

import org.spf4j.os.OperatingSystem;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.os.StdOutToStringProcessHandler;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogCollection;
import org.spf4j.test.log.TestLoggers;

/**
 * @author zoly
 */
public final class RuntimeTest {

  private static final Logger LOG = LoggerFactory.getLogger(RuntimeTest.class);

  @Test
  public void testHaveJnaPlatform() {
    Assert.assertTrue(Runtime.haveJnaPlatform());
  }


  @Test
  public void testMainClass() throws NoSuchMethodException {
    Class<?> mainClass = Runtime.getMainClass();
    Assert.assertNotNull(mainClass);
    Method method = mainClass.getMethod("main", String[].class);
    Assert.assertNotNull(method);
  }

  @Test
  public void testMaxOpenFiles() {
    Assume.assumeFalse(Runtime.isWindows());
    Assert.assertNotEquals(Integer.MAX_VALUE, OperatingSystem.getMaxFileDescriptorCount());
  }

  /**
   * Test of goDownWithError method, of class Runtime.
   */
  @Test
  public void testSomeParams() {
    LOG.debug("PID={}", Runtime.PID);
    LOG.debug("OSNAME={}", Runtime.OS_NAME);
    int nrOpenFiles = Runtime.getNrOpenFiles();
    LOG.debug("NR_OPEN_FILES={}", nrOpenFiles);
    Assert.assertThat(nrOpenFiles, Matchers.greaterThan(0));
    CharSequence lsofOutput = Runtime.getLsofOutput();
    LOG.debug("LSOF_OUT={}", lsofOutput);
    Assert.assertNotNull(lsofOutput);
    Assert.assertThat(lsofOutput.toString(), Matchers.containsString("jar"));
    LOG.debug("MAX_OPEN_FILES={}", OperatingSystem.getMaxFileDescriptorCount());
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
    LogCollection<Long> collect = TestLoggers.sys().collect(StdOutToStringProcessHandler.class.getName(), Level.ERROR,
            Level.ERROR, false, Collectors.counting());
    Runtime.jrun(RuntimeTest.TestError3.class, 10000);
    Assert.assertTrue(collect.get() > 0);
  }

  @Test(expected = InterruptedException.class, timeout = 30000)
  public void testExitCode4() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    final Thread t = Thread.currentThread();
    DefaultScheduler.INSTANCE.schedule(() -> {
      t.interrupt();
    }, 1, TimeUnit.SECONDS);
    Runtime.jrun(RuntimeTest.TestSleeping.class, 10000);
  }

  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  @Test(expected = CancellationException.class, timeout = 30000)
  public void testExitCode5() throws InterruptedException, ExecutionException, TimeoutException {
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch canCancel = new CountDownLatch(1);
    Future<?> submit = DefaultExecutor.INSTANCE.submit(new AbstractRunnable() {

      @Override
      public void doRun() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        try {
          canCancel.countDown();
          Runtime.jrun(RuntimeTest.TestError3.class, 10000);
        } catch (InterruptedException ex) {
          Throwables.writeTo(ex, System.err, Throwables.PackageDetail.SHORT);
          latch.countDown();
        } catch (Exception ex) {
          Throwables.writeTo(ex, System.err, Throwables.PackageDetail.SHORT);
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

  public static final class TestSleeping {

    @SuppressFBWarnings("MDM_THREAD_YIELD")
    public static void main(final String[] args) throws InterruptedException {
      Thread.sleep(60000);
    }
  }


  public static final class TestError {

    public static void main(final String[] args) {
      throw new RuntimeException();
    }
  }

  public static final class TestError2 {

    public static void main(final String[] args) {
      Thread.setDefaultUncaughtExceptionHandler((final Thread t, final Throwable e) -> {
        Throwables.writeTo(e, System.err, Throwables.PackageDetail.SHORT);
      });
      throw new RuntimeException();
    }
  }

  public static final class TestError3 {

    public static void main(final String[] args) {
      Thread.setDefaultUncaughtExceptionHandler((final Thread t, final Throwable e) -> {
        Throwables.writeTo(e, System.err, Throwables.PackageDetail.SHORT);
      });
      DefaultScheduler.INSTANCE.scheduleAtFixedRate(AbstractRunnable.NOP, 10, 10, TimeUnit.MILLISECONDS);
      throw new RuntimeException();
    }
  }

}
