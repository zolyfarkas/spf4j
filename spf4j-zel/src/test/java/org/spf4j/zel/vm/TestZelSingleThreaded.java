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
package org.spf4j.zel.vm;

import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public final class TestZelSingleThreaded {

  private static final Logger LOG = LoggerFactory.getLogger(TestZelSingleThreaded.class);

  @Test
  public void testPi() throws CompileException, ExecutionException, InterruptedException {
    for (int i = 0; i < 3; i++) {
      testPiImpl();
    }
  }

  public void testPiImpl() throws CompileException, ExecutionException, InterruptedException {
    String pi = "pi = func sync (x) {"
            + "term = func sync (k) {4 * (-1 ** k) / (2d * k + 1)};"
            + "for i = 0, result = 0; i < x; i = i + 1 { result = result + term(i) };"
            + "return result};"
            + "pi(x)";
    Program prog = Program.compile(pi, "x");
    long startTime = System.currentTimeMillis();
    Number result = (Number) prog.execute(100000);
    long endTime = System.currentTimeMillis();
    LOG.debug("zel pi = {} in {} ms", result, (endTime - startTime));
    // pi is 3.141592653589793
    Assert.assertEquals(3.141592653589793, result.doubleValue(), 0.0001);

  }

}
