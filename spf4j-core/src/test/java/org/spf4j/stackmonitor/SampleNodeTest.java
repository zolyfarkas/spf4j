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
import java.io.IOException;
import java.io.StringReader;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Methods;
import org.spf4j.base.Objects;
import org.spf4j.base.Pair;
import org.spf4j.base.avro.Method;

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
    StackTraceElement[] st1 = newSt1();
    SampleNode node1 = SampleNode.createSampleNode(st1);
    LOG.debug("Node 1", node1);

    Assert.assertEquals(4, node1.getNrNodes());

    StackTraceElement[] st2 = newSt2();
    SampleNode.addToSampleNode(node1, st2);
    LOG.debug("Node 1", node1);
    Assert.assertEquals(5, node1.getNrNodes());

    StackTraceElement[] st3 = newSt3();
    SampleNode.addToSampleNode(node1, st3);
    LOG.debug("Node 1", node1);

    StackTraceElement[] st4 = newSt4();
    SampleNode.addToSampleNode(node1, st4);


    SampleNode.addToSampleNode(node1, st1);

    LOG.debug("Node 1", node1);

    SampleNode agg = SampleNode.aggregate(node1, node1);
    LOG.debug("n1 + n2", agg);
    Assert.assertEquals(node1.getSampleCount() * 2, agg.getSampleCount());
    final Method method = Methods.getMethod("C1", "m3");
    Assert.assertEquals(node1.getSubNodes().get(method).getSampleCount() * 2,
            agg.getSubNodes().get(method).getSampleCount());

    StringBuilder sb = new StringBuilder();
    node1.writeTo(sb);
    LOG.debug("Serialized String", sb);

    SampleNode into = new SampleNode();
    SampleNode.parseInto(new StringReader(sb.toString()), into);
    Pair<Method, SampleNode> parsed = SampleNode.parse(new StringReader(sb.toString()));
    Assert.assertEquals(node1, parsed.getSecond());
    Assert.assertEquals(node1, Objects.clone(node1));
    Assert.assertEquals(node1, into);
    SampleNode node1Clone = SampleNode.clone(node1);
    Assert.assertEquals(node1, node1Clone);
    node1Clone.addToCount(5);
    Assert.assertNotEquals(node1, node1Clone);
    StringBuilder sb2 = new StringBuilder();
    node1.writeD3JsonTo(sb2);
    LOG.debug("Serialized D3 String", sb2);
    Pair<Method, SampleNode> parsed2 = SampleNode.parseD3Json(new StringReader(sb2.toString()));
    Assert.assertEquals(node1, parsed2.getSecond());

    SampleNode.traverse(parsed.getFirst(), parsed.getSecond(), (f, t, s) -> {
      LOG.debug("{} -> {} sampled {} times", f, t, s);
      return true;
    });

  }

  public StackTraceElement[] newSt4() {
    StackTraceElement[] st4 = new StackTraceElement[3];
    st4[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    st4[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
    st4[2] = new StackTraceElement("C1", "m4", "C1.java", 14);
    return st4;
  }

  public StackTraceElement[] newSt3() {
    StackTraceElement[] st3 = new StackTraceElement[3];
    st3[0] = new StackTraceElement("C2", "m1", "C2.java", 10);
    st3[1] = new StackTraceElement("C2", "m2", "C2.java", 11);
    st3[2] = new StackTraceElement("C2", "m3", "C2.java", 12);
    return st3;
  }

  public StackTraceElement[] newSt2() {
    StackTraceElement[] st2 = new StackTraceElement[1];
    st2[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    return st2;
  }

  public StackTraceElement[] newSt1() {
    StackTraceElement[] st1 = new StackTraceElement[3];
    st1[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    st1[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
    st1[2] = new StackTraceElement("C1", "m3", "C1.java", 12);
    return st1;
  }

  @Test
  public void testCreate() {
    StackTraceElement[] st1 = newSt1();
    SampleNode node1 = SampleNode.createSampleNode(st1);
    StackTraceElement[] st2 = newSt2();
    SampleNode.addToSampleNode(node1, st2);
    Assert.assertEquals(5, node1.getNrNodes());
    StackTraceElement[] st3 = newSt3();
    SampleNode.addToSampleNode(node1, st3);
    StackTraceElement[] st4 = newSt4();
    SampleNode.addToSampleNode(node1, st4);
    SampleNode.addToSampleNode(node1, st1);
    SampleNode node2 = SampleNode.create(node1);
    Assert.assertEquals(node1, node2);
  }


@Test
  public void testDiff() {
    StackTraceElement[] st1 = newSt1();
    SampleNode node1 = SampleNode.createSampleNode(st1);
    StackTraceElement[] st2 = newSt2();
    SampleNode.addToSampleNode(node1, st2);
    Assert.assertEquals(5, node1.getNrNodes());
    StackTraceElement[] st3 = newSt3();
    SampleNode.addToSampleNode(node1, st3);
    StackTraceElement[] st4 = newSt4();
    SampleNode.addToSampleNode(node1, st4);
    SampleNode.addToSampleNode(node1, st1);
    SampleNode result = SampleNode.diff(node1, node1);
    Assert.assertEquals(0, result.getSampleCount());
    Assert.assertEquals(0, result.size());
  }

@Test
  public void testDiff2() {
    StackTraceElement[] st1 = newSt1();
    SampleNode node1 = SampleNode.createSampleNode(st1);
    SampleNode sn1 = SampleNode.clone(node1);
    StackTraceElement[] st2 = newSt2();
    SampleNode.addToSampleNode(node1, st2);
    Assert.assertEquals(5, node1.getNrNodes());
    StackTraceElement[] st3 = newSt3();
    SampleNode.addToSampleNode(node1, st3);
    StackTraceElement[] st4 = newSt4();
    SampleNode.addToSampleNode(node1, st4);
    SampleNode.addToSampleNode(node1, st1);
    SampleNode node2 = SampleNode.clone(node1);
    SampleNode.addToSampleNode(node1, st1);
    SampleNode result = SampleNode.diff(node1, node2);
    Assert.assertEquals(sn1, result);
    Assert.assertEquals(node2, SampleNode.intersect(node1, node2));
    LOG.info("Annotated diff {}", SampleNode.diff_annotate(Methods.ROOT, node1, node2));
  }

}
