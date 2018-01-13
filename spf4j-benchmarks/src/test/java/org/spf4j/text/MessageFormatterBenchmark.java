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
package org.spf4j.text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.spf4j.base.Slf4jMessageFormatter;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 4)
public class MessageFormatterBenchmark {

  private static final ThreadLocal<StringBuilder> SB = new ThreadLocal<StringBuilder>() {
    @Override
    protected StringBuilder initialValue() {
      return new StringBuilder(128);
    }
  };

  private static final ThreadLocal<StringBuffer> SBF = new ThreadLocal<StringBuffer>() {
    @Override
    protected StringBuffer initialValue() {
      return new StringBuffer(128);
    }
  };

  @Benchmark
  public final CharSequence spf4jMessageFormatter() throws UnsupportedEncodingException, IOException {
    StringBuilder result = SB.get();
    result.setLength(0);
    org.spf4j.text.MessageFormat fmt = new org.spf4j.text.MessageFormat(
            "Here is some message wi parameter 0 = {0} and parameter 1 = {1} for testing performance", Locale.US);
    fmt.format(new Object[]{"[parameter 1]", "[parameter 2]"}, result, null);
    return result;
  }

  @Benchmark
  public final CharSequence jdkMessageFormatter() throws UnsupportedEncodingException, IOException {
    StringBuffer result = SBF.get();
    result.setLength(0);
    java.text.MessageFormat fmt = new java.text.MessageFormat(
            "Here is some message wi parameter 0 = {0} and parameter 1 = {1} for testing performance", Locale.US);
    fmt.format(new Object[]{"[parameter 1]", "[parameter 2]"}, result, null);
    return result;
  }

  @Benchmark
  public final CharSequence slf4jMessageFormatter() throws IOException {
    StringBuilder result = SB.get();
    result.setLength(0);
    Slf4jMessageFormatter.format(result,
            "Here is some message wi parameter 0 = {} and parameter 1 = {} for testing performance",
            "[parameter 1]", "[parameter 2]");
    return result;
  }

}
