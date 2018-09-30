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
import org.assertj.swing.core.Settings;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
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
import org.spf4j.test.log.ObservationAssert;
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
    ObservationAssert expectation = TestLoggers.sys().expectUncaughtException(2, TimeUnit.SECONDS,
            UncaughtExceptionDetail.hasThrowable((Matcher) Matchers.any(ExitException.class)));
    JFrame tFrame = GuiActionRunner.execute(() -> new Explorer());
    FrameFixture window = new FrameFixture(tFrame);
    Settings settings = window.robot().settings();
    settings.delayBetweenEvents(100);
    settings.eventPostingDelay(10);
    window.show(); // shows the frame to test
    Assert.assertTrue(window.isEnabled());
    JMenuItemFixture openFileMenuItem = window.menuItem(new GenericTypeMatcher<JMenuItem>(JMenuItem.class) {
      @Override
      protected boolean isMatching(final JMenuItem component) {
        return "Open".equals(component.getText());
      }
    });
    openFileMenuItem.click();
    Thread.sleep(100); // without this somehow things are not found.
    JFileChooserFixture fileChooser = window.fileChooser("openFileDialog");
    fileChooser.setCurrentDirectory(new File("src/test/resources"));
    fileChooser.selectFile(new File(
            "src/test/resources/com.google.common.io.AppendableWriterBenchmark.spf4jAppendable-Throughput.ssdump2")
            .getAbsoluteFile());
    fileChooser.approve();
    Assert.assertNotNull(window.internalFrame(
            "com.google.common.io.AppendableWriterBenchmark.spf4jAppendable-Throughput.ssdump2"));

    openFileMenuItem.click();
    Thread.sleep(100);
    fileChooser = window.fileChooser("openFileDialog");
    fileChooser.setCurrentDirectory(new File("src/test/resources"));
    Thread.sleep(100);
    fileChooser.selectFile(new File("src/test/resources/"
                    + "19156@ZMacBookPro.local.tsdb").getAbsoluteFile());
    fileChooser.approve();
    Assert.assertNotNull(window.internalFrame(
            "19156@ZMacBookPro.local.tsdb"));

    openFileMenuItem.click();
    Thread.sleep(100);
    fileChooser = window.fileChooser("openFileDialog");
    fileChooser.setCurrentDirectory(new File("src/test/resources"));
    Thread.sleep(100);
    fileChooser.selectFile(new File("src/test/resources/"
                    + "test8381720042200787335.tsdb2").getAbsoluteFile());
    fileChooser.approve();
    Assert.assertNotNull(window.internalFrame(
            "test8381720042200787335.tsdb2"));
    JMenuItemFixture openJsonText = window.menuItem(new GenericTypeMatcher<JMenuItem>(JMenuItem.class) {
      @Override
      protected boolean isMatching(final JMenuItem component) {
        return "FromText".equals(component.getText());
      }
    });
    openJsonText.click();
    Thread.sleep(100);
    DialogFixture dialog = window.dialog("fromTextDialog");
    dialog.textBox("textBox").setText("{\"ROOT@49924@ZMacBookPro-2.local\":10,"
            + "\"c\":[{\"m3@C2\":2,\"c\":[{\"m2@C2\":2,\"c\":[{\"m1@C2\":2}]}]},{\"m1@C1\":2},"
            + "{\"m3@C1\":4,\"c\":[{\"m2@C1\":4,\"c\":[{\"m1@C1\":4}]}]},"
            + "{\"m4@C1\":2,\"c\":[{\"m2@C1\":2,\"c\":[{\"m1@C1\":2}]}]}]}");
    dialog.button("display").click();
    Assert.assertNotNull(window.internalFrame("text entry"));
    window.close();
    window.cleanUp();
    expectation.assertObservation();
  }

}
