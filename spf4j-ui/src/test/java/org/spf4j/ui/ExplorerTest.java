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
import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.fixture.JMenuItemFixture;
import org.assertj.swing.security.ExitException;
import org.assertj.swing.security.NoExitSecurityManagerInstaller;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spf4j.test.log.AsyncObservationAssert;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.log.UncaughtExceptionDetail;

/**
 * @author Zoltan Farkas
 */
public class ExplorerTest {

  private static NoExitSecurityManagerInstaller installNoExitSecurityManager;

  @BeforeClass
  public static void setUpOnce() {
     FailOnThreadViolationRepaintManager.install();
    installNoExitSecurityManager = NoExitSecurityManagerInstaller.installNoExitSecurityManager();
  }

  @AfterClass
  public static void tearDown() {
    installNoExitSecurityManager.uninstall();
  }

  @Test
  @SuppressFBWarnings("MDM_THREAD_YIELD") // need to since assertj does not seem to apply things otherwise...
  public void testExplorer() throws InterruptedException {
    AsyncObservationAssert expectation = TestLoggers.sys().expectUncaughtException(
            UncaughtExceptionDetail.hasThrowable((Matcher) Matchers.any(ExitException.class)));
    JFrame tFrame = GuiActionRunner.execute(() -> new Explorer());
    FrameFixture window = new FrameFixture(tFrame);
    window.robot().settings().delayBetweenEvents(200);
    window.show(); // shows the frame to test
    Assert.assertTrue(window.isEnabled());
    JMenuItemFixture openFileMenuItem = window.menuItem(new GenericTypeMatcher<JMenuItem>(JMenuItem.class) {
      @Override
      protected boolean isMatching(final JMenuItem component) {
        return "Open".equals(component.getText());
      }
    });
    openFileMenuItem.click();
    JFileChooserFixture fileChooser = window.fileChooser("openFileDialog");
    fileChooser.setCurrentDirectory(new File("src/test/resources"));
    fileChooser.selectFile(new File(
            "src/test/resources/com.google.common.io.AppendableWriterBenchmark.spf4jAppendable-Throughput.ssdump2")
            .getAbsoluteFile());
    fileChooser.approve();
    Assert.assertNotNull(window.internalFrame(
            "com.google.common.io.AppendableWriterBenchmark.spf4jAppendable-Throughput.ssdump2"));

    openFileMenuItem.click();
    fileChooser = window.fileChooser("openFileDialog");
    fileChooser.setCurrentDirectory(new File("src/test/resources"));
    Thread.sleep(100);
    fileChooser.selectFile(new File("src/test/resources/"
                    + "19156@ZMacBookPro.local.tsdb").getAbsoluteFile());
    fileChooser.approve();
    Assert.assertNotNull(window.internalFrame(
            "19156@ZMacBookPro.local.tsdb"));
    window.close();
    window.cleanUp();
    expectation.assertObservation(2, TimeUnit.SECONDS);
  }

}
