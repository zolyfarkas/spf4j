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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.test.log.TestUtils;

/**
 *
 * @author Zoltan Farkas
 */

public class FlameStackPanelTest {


  private static final SampleNode NODES;

  static {
    try (InputStream is = Resources.getResource(
            "com.google.common.io.AppendableWriterBenchmark.spf4jAppendable-Throughput.ssdump2").openStream()) {
      NODES = Converter.load(is);
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }



  @Test
  public void testLoadingHotStackPanel() throws IOException, InterruptedException {
    HotFlameStackPanel panel = new HotFlameStackPanel(NODES);
    testPanel(panel);
  }

  @Test
  public void testLoadingStackPanel() throws IOException, InterruptedException {
    FlameStackPanel panel = new FlameStackPanel(NODES);
    testPanel(panel);
  }

  public final void testPanel(final JPanel panel) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    CountDownLatch closeLatch = new CountDownLatch(1);
    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("Bevel Arrows");

      frame.add(panel, BorderLayout.CENTER);
      frame.setSize(800, 400);
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
