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
package org.spf4j.stackmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public final class SimpleStackCollectorTest {

  private static final Logger LOG = LoggerFactory.getLogger(SimpleStackCollectorTest.class);

  /**
   * Test of sample method, of class SimpleStackCollector.
   */
  @Test
  @SuppressFBWarnings({"PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", "ES_COMPARING_STRINGS_WITH_EQ"})
  public void testSample() {
    StackCollector instance = new StackCollectorImpl();
    StackTraceElement[] st1 = new StackTraceElement[3];
    st1[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    st1[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
    st1[2] = new StackTraceElement("C1", "m3", "C1.java", 12);
    instance.collect(st1);
    LOG.debug("Collector = {}", instance);
    Assert.assertEquals(4, instance.get().getNrNodes());

    StackTraceElement[] st2 = new StackTraceElement[1];
    st2[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    instance.collect(st2);
    LOG.debug("Collector = {}", instance);
    Assert.assertEquals(5, instance.get().getNrNodes());

    StackTraceElement[] st3 = new StackTraceElement[3];
    st3[0] = new StackTraceElement("C2", "m1", "C2.java", 10);
    st3[1] = new StackTraceElement("C2", "m2", "C2.java", 11);
    st3[2] = new StackTraceElement("C2", "m3", "C2.java", 12);
    instance.collect(st3);
    LOG.debug("Collector = {}", instance);

    StackTraceElement[] st4 = new StackTraceElement[3];
    st4[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    st4[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
    st4[2] = new StackTraceElement("C1", "m4", "C1.java", 14);
    instance.collect(st4);

    LOG.debug("Collector = {}", instance);
    Thread currentThread = Thread.currentThread();

    Assert.assertSame(currentThread.getStackTrace()[0].getClassName(),
            currentThread.getStackTrace()[0].getClassName());
    Assert.assertSame(currentThread.getStackTrace()[0].getMethodName(),
            currentThread.getStackTrace()[0].getMethodName());

  }

}
