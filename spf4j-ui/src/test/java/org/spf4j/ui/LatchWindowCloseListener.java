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

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Zoltan Farkas
 */
public final class LatchWindowCloseListener implements WindowListener {

  private final CountDownLatch closeLatch;

  public LatchWindowCloseListener(final CountDownLatch closeLatch) {
    this.closeLatch = closeLatch;
  }

  @Override
  public void windowOpened(final WindowEvent e) {
  }

  @Override
  public void windowClosing(final WindowEvent e) {
  }

  @Override
  public void windowClosed(final WindowEvent e) {
    closeLatch.countDown();
  }

  @Override
  public void windowIconified(final WindowEvent e) {
  }

  @Override
  public void windowDeiconified(final WindowEvent e) {
  }

  @Override
  public void windowActivated(final WindowEvent e) {
  }

  @Override
  public void windowDeactivated(final WindowEvent e) {
  }

  @Override
  public String toString() {
    return "LatchWindowCloseListener{" + "closeLatch=" + closeLatch + '}';
  }

}
