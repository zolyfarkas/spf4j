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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Zoltan Farkas
 */
public class Arrow2DUnitTest {

  @BeforeClass
  public static void setUpOnce() {
    FailOnThreadViolationRepaintManager.install();
  }

  @Test
  public void testArrowDraw() throws InterruptedException {

    JFrame tFrame = GuiActionRunner.execute(() -> {
      JFrame frame = new JFrame("Arrow");

      frame.add(new JPanel() {
        @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
        public void paintComponent(final Graphics g) {
          new Arrow2D(0, 0, 100, 100).draw((Graphics2D) g);
        }
      }, BorderLayout.CENTER);
      frame.setSize(800, 400);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setVisible(true);
      return frame;

    });
    FrameFixture window = new FrameFixture(tFrame);
    window.show(); // shows the frame to test
    Assert.assertTrue(window.isEnabled());
    window.close();
    window.cleanUp();
  }

}
