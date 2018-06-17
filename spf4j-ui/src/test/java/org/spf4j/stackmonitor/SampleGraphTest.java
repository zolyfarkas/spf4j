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

import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Method;

/**
 *
 * @author Zoltan Farkas
 */
public class SampleGraphTest {

  private static final Logger LOG = LoggerFactory.getLogger(SampleGraphTest.class);

  /**
   * 2 sample case:
   * a -> b
   * b -> a
   *
   * @throws IOException
   */
  @Test
  @SuppressFBWarnings("LO_INCORRECT_NUMBER_OF_ANCHOR_PARAMETERS")
  public void testCycleAggLogic() throws IOException {
    SampleNode node = SampleNode.createSampleNode(new StackTraceElement[] {
      new StackTraceElement("C", "a", "C.java", -1),
      new StackTraceElement("C", "b", "C.java", -1)
    });
    SampleNode.addToSampleNode(node, new StackTraceElement[] {
      new StackTraceElement("C", "b", "C.java", -1),
      new StackTraceElement("C", "a", "C.java", -1)
    });
    LOG.debug("samples", node);
    SampleGraph sg = new SampleGraph(Method.ROOT, node);
    SampleGraph.AggSample aggRootVertex = sg.getAggRootVertex();
    LOG.debug("Root = {}", aggRootVertex);
    Set<SampleGraph.AggSample> children = sg.getChildren(aggRootVertex);
    LOG.debug("Children = {}", children);
    for (SampleGraph.AggSample child : children) {
      LOG.debug("Children Children = {}", sg.getChildren(child));
    }
    Assert.assertEquals(2, sg.getAggNode(new SampleGraph.SampleKey(Method.getMethod("C", "a"), 0)).getNrSamples());
    Assert.assertEquals(2, sg.getAggNode(new SampleGraph.SampleKey(Method.getMethod("C", "b"), 0)).getNrSamples());
  }

  @Test
  public void testAggLogic() throws IOException {
    SampleNode samples = org.spf4j.ssdump2.Converter.load(Resources.getResource(
            "org.spf4j.concurrent.ThreadPoolBenchmark.spfLifoTpBenchmark-Throughput_m4.ssdump2").openStream());
    Method method = Method.getMethod("org.spf4j.concurrent.LifoThreadPoolExecutorSQP$QueuedThread", "doRun");
    AtomicInteger ai = new AtomicInteger();
    SampleNode.traverse(Method.ROOT, samples, (f, t, s) -> {
      if (f.equals(method)) {
        LOG.debug("from = {}, to = {}, samples = {}", f, t, s);
        ai.getAndIncrement();
      }
      return true;
    });
    SampleGraph sg = new SampleGraph(Method.ROOT, samples);
    SampleGraph.SampleKey sampleKey = new SampleGraph.SampleKey(
            method, 0);
    SampleGraph.AggSample aggNode = sg.getAggNode(sampleKey);
    Set<SampleGraph.AggSample> children = sg.getChildren(aggNode);
    LOG.debug("parent = {}, children = {}", aggNode, children);
    Assert.assertEquals(ai.get(), children.size());
  }

}
