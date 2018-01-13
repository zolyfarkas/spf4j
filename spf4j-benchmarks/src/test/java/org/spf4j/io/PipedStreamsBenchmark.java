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
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.Strings;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 1)
public class PipedStreamsBenchmark {

  private static final String TEST_STR = "asfsdfhjgsdjhfgsjhdgfjhsdgfjhgsdjhfgjsdhgkjfsdkhf34hfHGHDG"
          + "SFDGHJJIU&^%ERSDFGVNHKJU&^%!#@#$%^&*()OJHGCXFDGHJUYTRWERTGFHHJYREWRDFGHJUYTredscxvbbhuytdsdfbvnmjhgfd"
          + "dkjhfkjsdhfkdskgfskjdhfjkdfghsdkjhfglskdfhjgkldfhgksjdfhgklhsdfkghklsfdhgkdfhlkfghfslkdjhgklsdhkghs";

  @Benchmark
  public void testSpf4Pipe() throws IOException {
    testSpf(TEST_STR, 64);
  }

  @Benchmark
  public void testjavaPipe() throws IOException {
    testJdk(TEST_STR, 64);
  }

  private void testSpf(final String testStr, final int buffSize) throws IOException {
    final PipedOutputStream pos = new PipedOutputStream(buffSize);
    final InputStream pis = pos.getInputStream();
    DefaultExecutor.INSTANCE.execute(new AbstractRunnable() {

      @Override
      public void doRun() throws Exception {
        try (OutputStream os = pos) {
          final byte[] utf8 = Strings.toUtf8(testStr);
          os.write(utf8[0]);
          os.write(utf8, 1, 10);
          os.write(utf8, 11, 10);
          os.write(utf8, 21, utf8.length - 21);
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
    Assert.assertEquals(testStr, sb.toString());
  }

  private void testJdk(final String testStr, final int buffSize) throws IOException {
    final java.io.PipedOutputStream pos = new java.io.PipedOutputStream();
    final java.io.PipedInputStream pis = new java.io.PipedInputStream(pos, buffSize);
    DefaultExecutor.INSTANCE.execute(new AbstractRunnable() {

      @Override
      public void doRun() throws Exception {
        try (OutputStream os = pos) {
          final byte[] utf8 = Strings.toUtf8(testStr);
          os.write(utf8[0]);
          os.write(utf8, 1, 10);
          os.write(utf8, 11, 10);
          os.write(utf8, 21, utf8.length - 21);
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
    Assert.assertEquals(testStr, sb.toString());
  }

}
