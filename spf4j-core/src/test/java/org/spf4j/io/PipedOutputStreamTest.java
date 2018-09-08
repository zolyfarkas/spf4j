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
package org.spf4j.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.IntMath;
import org.spf4j.base.Strings;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author zoly
 */
public class PipedOutputStreamTest {

  private static final Logger LOG = LoggerFactory.getLogger(PipedOutputStreamTest.class);

  @Test
  public void testStreamPiping() throws IOException {
    test("This is a super cool, mega dupper test string for testing piping..........E", 8, false);
    final IntMath.XorShift32 random = new IntMath.XorShift32();
    for (int i = 0; i < 100; i++) {
      int nrChars = Math.abs(random.nextInt() % 100000);
      StringBuilder sb = generateTestStr(nrChars);
      test(sb.toString(), Math.abs(random.nextInt() % 10000) + 2, false);
    }
    test(generateTestStr(133).toString(), 2, false);
  }

  public static StringBuilder generateTestStr(final int nrChars) {
    final IntMath.XorShift32 random = new IntMath.XorShift32();
    StringBuilder sb = new StringBuilder(nrChars);
    for (int i = 0; i < nrChars; i++) {
      sb.append((char) Math.abs(random.nextInt() % 100) + 20);
    }
    return sb;
  }

  public static void test(final String testStr, final int buffSize, final boolean buffered) throws IOException {
    final PipedOutputStream pos = new PipedOutputStream(buffSize);
    final InputStream pis;
    if (buffered) {
      pis = new MemorizingBufferedInputStream(pos.getInputStream());
    } else {
      pis = pos.getInputStream();
    }

    DefaultExecutor.INSTANCE.execute(new AbstractRunnable() {

      @Override
      public void doRun() throws Exception {
        try (OutputStream os = pos) {
          final byte[] utf8 = Strings.toUtf8(testStr);
          os.write(utf8[0]);
          os.write(utf8, 1,  utf8.length - 1);
        }
      }
    });
    StringBuilder sb = new StringBuilder();
    try (InputStream is = pis) {
      byte[] buffer = new byte[1024];
      int read;
      while ((read = is.read(buffer)) > 0) {
        sb.append(Strings.fromUtf8(buffer, 0, read));
      }
    }
    Assert.assertEquals("buffSize = " + buffSize + ", buffered = " + buffered, testStr, sb.toString());
  }

  @Test
  public void testNoReaderBehaviour() throws IOException, InterruptedException,
          ExecutionException, TimeoutException {
    PipedOutputStream os = new PipedOutputStream(1024);
    Future<Integer> nr;
    int j = 0;
    try (PipedOutputStream pos = os) {
      pos.write(123);
      LOG.debug("write {}", j);
      j++;
      nr = DefaultExecutor.instance().submit(() -> {
        try (InputStream is = pos.getInputStream()) {
          int i = 0;
          int val;
          while ((val = is.read()) >= 0) {
            Assert.assertEquals(123, val);
            LOG.debug("read {}", i);
            i++;
          }
          return i;
        }
      });
      for (; j < 2000; j++) {
        pos.write(123);
        LOG.debug("write {}", j);
      }
    }
    LOG.debug("os = {}", os);
    Assert.assertEquals(j, nr.get(3, TimeUnit.SECONDS).intValue());
  }



  @Test
  public void testNoReaderBehaviour2() throws IOException {
    try (PipedOutputStream pos = new PipedOutputStream(1024)) {
      try (InputStream is = pos.getInputStream()) {
        pos.write(123);
        pos.flush();
        int val = is.read();
        Assert.assertEquals(123, val);
      }
      pos.write(123);
    }
  }

  @Test(timeout = 2000, expected = IOTimeoutException.class)
  public void testNoReaderTimeout() throws IOException {
    try (ExecutionContext ctx = ExecutionContexts.start("tt", 1, TimeUnit.MILLISECONDS);
            PipedOutputStream pos = new PipedOutputStream(10)) {
      try (InputStream is = pos.getInputStream()) {
        for (int i = 0; i < 11; i++) {
          pos.write(123);
        }
      }
    }
  }

  @Test
  public void testCloseWithReason() throws IOException {
    try (PipedOutputStream pos = new PipedOutputStream(10)) {
      pos.write(123);
      IOException ex = new IOException();
      pos.close(ex);
      try {
        pos.write(123);
        Assert.fail();
      } catch (IOException ex2) {
        Assert.assertEquals(ex, ex2.getCause());
      }
      try (InputStream is = pos.getInputStream()) {
        Assert.assertEquals(123, is.read());
      }
    }
  }

}
