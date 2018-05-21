/*
 * Copyright 2018 SPF4J.
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

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Method;
import org.spf4j.ds.Graph;
import org.spf4j.ds.Traversals;
import org.spf4j.stackmonitor.SampleGraph.SampleKey;

/**
 *
 * @author Zoltan Farkas
 */
public class AggGraphTest {

  private static final Logger LOG = LoggerFactory.getLogger(AggGraphTest.class);

  @Test
  public void testSomeMethod() {
    StackTraceElement[] st1 = new StackTraceElement[3];
    st1[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    st1[1] = new StackTraceElement("C1", "m2", "C1.java", 11);
    st1[2] = new StackTraceElement("C1", "m3", "C1.java", 12);
    SampleNode node1 = SampleNode.createSampleNode(st1);
    StackTraceElement[] st2 = new StackTraceElement[1];
    st2[0] = new StackTraceElement("C1", "m1", "C1.java", 10);
    SampleNode.addToSampleNode(node1, st2);
    StackTraceElement[] st3 = new StackTraceElement[3];
    st3[0] = new StackTraceElement("C2", "m1", "C2.java", 10);
    st3[1] = new StackTraceElement("C2", "m2", "C2.java", 11);
    st3[2] = new StackTraceElement("C2", "m3", "C2.java", 12);
    SampleNode.addToSampleNode(node1, st3);
    Graph<SampleKey, SampleNode.InvocationCount> graph = AggGraph.toGraph(node1);
    Traversals.traverse(graph, new SampleGraph.SampleKey(Method.ROOT, 0),
            (final SampleKey vertex, final Map<SampleNode.InvocationCount, SampleKey> edges) -> {
              LOG.debug("Method: {} from {}", vertex, edges);
            }, true);
    Assert.assertNotNull(node1);
  }

}
