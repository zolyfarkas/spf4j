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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 4)
public class StringsBenchmark {

  public static final String TEST_STRING;

  static {
    java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2500);
    OutputStream bos = encoder.wrap(byteArrayOutputStream);
    for (int i = 0; i < 2400; i++) {
      try {
        bos.write(ThreadLocalRandom.current().nextInt(256));
      } catch (IOException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
    TEST_STRING = "1234567890" + new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
  }

  @Benchmark
  public final byte[] optimizedSubStringDecode() throws UnsupportedEncodingException {
    return Base64.decodeBase64(CharSequences.subSequence(TEST_STRING, 10, TEST_STRING.length()));
  }

  @Benchmark
  public final byte[] subStringDecode() {
    return Base64.decodeBase64(TEST_STRING.subSequence(10, TEST_STRING.length()));
  }

}
