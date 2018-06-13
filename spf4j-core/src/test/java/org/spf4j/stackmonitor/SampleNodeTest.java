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

import org.spf4j.base.Method;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Objects;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("LO_INCORRECT_NUMBER_OF_ANCHOR_PARAMETERS")
public final class SampleNodeTest {

  private static final Logger LOG = LoggerFactory.getLogger(SampleNodeTest.class);

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testSampleNode() throws IOException {

    LOG.debug("sample");
    StackTraceElement[] st1 = new StackTraceElement[3];
    st1[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    st1[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
    st1[2] = new StackTraceElement("C1", "m3", "C1.java", 12);
    SampleNode node1 = SampleNode.createSampleNode(st1);
    SampleNode node2 = new SampleNode(st1, st1.length - 1);
    LOG.debug("Node 1", node1);
    LOG.debug("Node 2", node2);

    Assert.assertEquals(4, node1.getNrNodes());
    Assert.assertEquals(4, node2.getNrNodes());

    StackTraceElement[] st2 = new StackTraceElement[1];
    st2[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    SampleNode.addToSampleNode(node1, st2);
    node2.addSample(st2, st2.length - 1);
    LOG.debug("Node 1", node1);
    LOG.debug("Node 2", node2);
    Assert.assertEquals(5, node1.getNrNodes());
    Assert.assertEquals(5, node2.getNrNodes());

    StackTraceElement[] st3 = new StackTraceElement[3];
    st3[0] = new StackTraceElement("C2", "m1", "C2.java", 10);
    st3[1] = new StackTraceElement("C2", "m2", "C2.java", 11);
    st3[2] = new StackTraceElement("C2", "m3", "C2.java", 12);
    SampleNode.addToSampleNode(node1, st3);
    node2.addSample(st3, st3.length - 1);
    LOG.debug("Node 1", node1);
    LOG.debug("Node 2", node2);

    StackTraceElement[] st4 = new StackTraceElement[3];
    st4[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    st4[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
    st4[2] = new StackTraceElement("C1", "m4", "C1.java", 14);
    SampleNode.addToSampleNode(node1, st4);
    node2.addSample(st4, st4.length - 1);

    SampleNode.addToSampleNode(node1, st1);
    node2.addSample(st1, st1.length - 1);

    LOG.debug("Node 1", node1);
    LOG.debug("Node 2", node2);
    Assert.assertEquals(node1.toString(), node2.toString());

    SampleNode agg = SampleNode.aggregate(node1, node2);
    LOG.debug("n1 + n2", agg);
    Assert.assertEquals(node1.getSampleCount() + node2.getSampleCount(), agg.getSampleCount());
    final Method method = Method.getMethod("C1", "m3");
    Assert.assertEquals(node1.getSubNodes().get(method).getSampleCount()
            + node2.getSubNodes().get(method).getSampleCount(),
            agg.getSubNodes().get(method).getSampleCount());

    StringBuilder sb = new StringBuilder();
    node1.writeTo(sb);
    LOG.debug("Serialized String", sb);
    Pair<Method, SampleNode> parsed = SampleNode.parse(new StringReader(sb.toString()));
    Assert.assertEquals(node1, parsed.getSecond());
    Assert.assertEquals(node1, Objects.clone(node1));

    SampleNode.traverse(parsed.getFirst(), parsed.getSecond(), (f, t, s) -> {
      LOG.debug("{} -> {} sampled {} times", f, t, s);
      return true;
    });

  }



}
