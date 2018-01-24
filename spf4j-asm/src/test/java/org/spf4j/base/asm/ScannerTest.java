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
package org.spf4j.base.asm;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public final class ScannerTest {

  private static final Logger LOG = LoggerFactory.getLogger(ScannerTest.class);

  public static class A {

    /**
     * Test method for override, this implementation returns a Object.
     * @return some test object.
     */
    public Object getValue() {
      return new Object();
    }
  }

  public static final class B extends A {

    @Override
    public String getValue() {
      return "B";
    }
  }

  public static final String DEFAULT_SS_DUMP_FILE_NAME_PREFIX =
            System.getProperty("spf4j.perf.ms.defaultSsdumpFilePrefix", ManagementFactory.getRuntimeMXBean().getName());


  public void testSomeMethod() {
    // read a System property
    System.getProperty("some.property", "default value");
    Integer.getInteger("someInt.value", 3);
    Long.getLong("someLong.value", 5);
  }

  public int method() {
    /**
     * another
     */
    return Integer.getInteger("spf4j.custom.prop2", 1);
  }

  @Test
  public void testScan() throws NoSuchMethodException, IOException {

    List<Invocation> findUsages = Scanner.findUsages(ScannerTest.class,
            ImmutableSet.of(A.class.getDeclaredMethod("getValue")));
    LOG.debug("Usages: {}", findUsages);
    final ImmutableSet<Method> lookFor = ImmutableSet.of(System.class.getDeclaredMethod("getProperty", String.class),
            System.class.getDeclaredMethod("getProperty", String.class, String.class),
            Integer.class.getDeclaredMethod("getInteger", String.class),
            Integer.class.getDeclaredMethod("getInteger", String.class, int.class),
            Integer.class.getDeclaredMethod("getInteger", String.class, Integer.class),
            Long.class.getDeclaredMethod("getLong", String.class),
            Long.class.getDeclaredMethod("getLong", String.class, Long.class),
            Long.class.getDeclaredMethod("getLong", String.class, long.class),
            Boolean.class.getDeclaredMethod("getBoolean", String.class));
    List<Invocation> findUsages2 = Scanner.findUsages(ScannerTest.class, lookFor);
    LOG.debug("Scan 1 = {}", findUsages2);
    List<Invocation> findUsages3 = Scanner.findUsages(ScannerTest.class.getPackage().getName(), lookFor);
    LOG.debug("Scan 2 = {}", findUsages3);
    Assert.assertThat(findUsages2, CoreMatchers.hasItem(
            Matchers.allOf(
                    Matchers.hasProperty("caleeMethodName",
                            Matchers.equalTo("method")),
                    Matchers.hasProperty("invokedMethod", Matchers.hasProperty("name", Matchers.equalTo("getInteger"))),
                    Matchers.hasProperty("parameters", Matchers.arrayContaining("spf4j.custom.prop2", 1)))));

    Assert.assertThat(findUsages2, CoreMatchers.hasItem(
            Matchers.allOf(
                    Matchers.hasProperty("caleeMethodName",
                            Matchers.equalTo("testSomeMethod")),
                    Matchers.hasProperty("invokedMethod",
                            Matchers.hasProperty("name", Matchers.equalTo("getProperty"))),
                    Matchers.hasProperty("parameters", Matchers.arrayContaining("some.property", "default value")))));

     Assert.assertThat(findUsages2, CoreMatchers.hasItem(
            Matchers.allOf(
                    Matchers.hasProperty("caleeMethodName",
                            Matchers.equalTo("testSomeMethod")),
                    Matchers.hasProperty("invokedMethod", Matchers.hasProperty("name", Matchers.equalTo("getLong"))),
                    Matchers.hasProperty("parameters", Matchers.arrayContaining("someLong.value", 5L)))));


  }

}
