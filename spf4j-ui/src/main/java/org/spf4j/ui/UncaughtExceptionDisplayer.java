/*
 * Copyright 2019 SPF4J.
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
import gnu.trove.set.hash.THashSet;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.spf4j.base.Pair;
import org.spf4j.base.SysExits;
import org.spf4j.base.Throwables;

public final class UncaughtExceptionDisplayer implements Thread.UncaughtExceptionHandler {

  private static final Lock ERR_LOCK = new ReentrantLock();

  private final Set<Pair<Class<? extends Throwable>, String>> seen = new THashSet<>();


  @Nullable
  private static Frame findActiveFrame() {
    Frame[] frames = JFrame.getFrames();
    for (Frame frame : frames) {
      if (frame.isVisible()) {
        return frame;
      }
    }
    return null;
  }

  @Override
  @SuppressFBWarnings("DM_EXIT")
  public void uncaughtException(final Thread t, final Throwable e) {
    String message = e.getMessage();
    if (e instanceof IllegalStateException && message != null
            && message.contains("Buffers have not been created")) {
      // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6933331
      Throwables.writeTo(e, System.err, Throwables.PackageDetail.SHORT);
      return;
    }
    Pair<Class<? extends Throwable>, String> exData = Pair.of(e.getClass(), message);
    if (seen.contains(exData)) {
      return;
    }
    if (!GraphicsEnvironment.isHeadless()) {
      Frame frame = findActiveFrame();
      if (frame != null) {
        try {
          if (ERR_LOCK.tryLock(1, TimeUnit.MILLISECONDS)) {
            try {
              JTextArea textArea = new JTextArea(Throwables.toString(e));
              JScrollPane scrollPane = new JScrollPane(textArea);
              textArea.setLineWrap(true);
              textArea.setWrapStyleWord(true);
              scrollPane.setPreferredSize(new Dimension(500, 500));
              int reply = JOptionPane.showConfirmDialog(frame, scrollPane, "Exception, ignore?",
                      JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
               if (reply == JOptionPane.YES_OPTION) {
                 seen.add(exData);
               }
            } finally {
              ERR_LOCK.unlock();
            }
          } else {
            Throwables.writeTo(e, System.err, Throwables.PackageDetail.SHORT);
          }
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return;
        }
      } else {
        Throwables.writeTo(e, System.err, Throwables.PackageDetail.SHORT);
      }
    } else {
      Throwables.writeTo(e, System.err, Throwables.PackageDetail.SHORT);
    }
    if (Throwables.isNonRecoverable(e)) {
      System.exit(SysExits.EX_SOFTWARE.exitCode());
    }
  }

  @Override
  public String toString() {
    return "UncaughtExceptionDisplayer{" + "seen=" + seen + '}';
  }

}
