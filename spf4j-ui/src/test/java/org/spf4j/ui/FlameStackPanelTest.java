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
package org.spf4j.ui;

import com.google.common.io.Resources;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Method;
import org.spf4j.base.Pair;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.SampleGraph;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.test.log.TestUtils;

/**
 *
 * @author Zoltan Farkas
 */
public class FlameStackPanelTest {

  private static final Logger LOG = LoggerFactory.getLogger(FlameStackPanelTest.class);

  private static final SampleNode NODES
          = loadSsdump2("com.google.common.io.AppendableWriterBenchmark.spf4jAppendable-Throughput.ssdump2");

  private static final SampleNode NODES2
          = loadSsdump("61160@ZMacBookPro.local_20130826T204120-0400_20130826T204128-0400.ssdump");

  public static SampleNode loadSsdump2(final String resourceName) {
    try (InputStream is = Resources.getResource(resourceName).openStream()) {
      return Converter.load(is);
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  public static SampleNode loadSsdump(final String resourceName) {
    try (InputStream is = Resources.getResource(resourceName).openStream()) {
      return Explorer.loadLegacyFormat(is);
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @Test
  public void testLoadingHotStackPanel() throws IOException, InterruptedException {
    HotFlameStackPanel panel = new HotFlameStackPanel(Method.ROOT, NODES, new LinkedList<>());
    testPanel(panel);
  }


  @Test
  public void testSampleGraph() throws IOException, InterruptedException {
    LOG.debug("Graph = {}", NODES2);
    SampleGraph sg = new SampleGraph(Method.ROOT, NODES2);
    SampleGraph.AggSample aggRootVertex = sg.getAggRootVertex();
    SampleGraph.AggSample child = sg.getChildren(aggRootVertex).iterator().next();
    Assert.assertTrue(sg.isParentDescendant(aggRootVertex, child));
  }


  @Test
  public void testLoadingHotStackPanel2() throws IOException, InterruptedException {
    LOG.debug("Graph = {}", NODES2);
    HotFlameStackPanel panel = new HotFlameStackPanel(Method.ROOT, NODES2, new LinkedList<>());
    testPanel(panel);
  }

  @Test
  public void testHotStackPanelCycle() throws IOException, InterruptedException {
    Pair<Method, SampleNode> parse = SampleNode.parse(new StringReader("{\"ROOT@65406@ZMacBookPro-2.local\":2,\"c\":"
            + "[{\"a@C\":1,\"c\":[{\"b@C\":1}]},{\"b@C\":1,\"c\":[{\"a@C\":1}]}]}"));
    HotFlameStackPanel panel = new HotFlameStackPanel(parse.getFirst(), parse.getSecond(), new LinkedList<>());
    testPanel(panel);
  }



  @Test
  public void testLoadingStackPanel() throws IOException, InterruptedException {
    FlameStackPanel panel = new FlameStackPanel(Method.ROOT, NODES, new LinkedList<>());
    testPanel(panel);
  }

  public final void testPanel(final JPanel panel) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    CountDownLatch closeLatch = new CountDownLatch(1);
    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("CallGraphs");

      frame.add(panel, BorderLayout.CENTER);
      frame.setSize(800, 1600);
      frame.addWindowListener(new LatchWindowCloseListener(closeLatch));
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setVisible(true);
      latch.countDown();
    });
    Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
    if (TestUtils.isExecutedFromIDE()) {
      Assert.assertTrue(closeLatch.await(1, TimeUnit.HOURS));
    }
  }

}
