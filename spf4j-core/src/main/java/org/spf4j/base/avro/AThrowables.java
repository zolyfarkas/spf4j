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
package org.spf4j.base.avro;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.spf4j.base.Throwables;
import static org.spf4j.base.Throwables.CAUSE_CAPTION;
import static org.spf4j.base.Throwables.SUPPRESSED_CAPTION;
import static org.spf4j.base.Throwables.writeAbreviatedClassName;
import org.spf4j.ds.IdentityHashSet;

/**
 *
 * @author Zoltan Farkas
 */
public final class AThrowables {

  private AThrowables() { }

    public static void writeTo(final RemoteException t, final Appendable to, final Throwables.PackageDetail detail,
          final boolean abbreviatedTraceElement, final String prefix) throws IOException {
      to.append(prefix);
      writeMessageString(to, t);
      to.append('\n');
      writeThrowableDetails(t.getRemoteCause(), to, detail, abbreviatedTraceElement, prefix);
    }

   public static void writeMessageString(final Appendable to, final RemoteException t) throws IOException {
    to.append(t.getClass().getName());
    to.append('@');
    to.append(t.getSource());
    String message = t.getMessage();
    if (message != null) {
      to.append(':').append(message);
    }
  }

   public static void writeTo(final Throwable t, final Appendable to, final Throwables.PackageDetail detail,
          final boolean abbreviatedTraceElement, final String prefix) throws IOException {
    to.append(prefix);
    writeMessageString(to, t);
    to.append('\n');
    writeThrowableDetails(t, to, detail, abbreviatedTraceElement, prefix);
  }

  public static void writeThrowableDetails(final Throwable t, final Appendable to,
          final Throwables.PackageDetail detail, final boolean abbreviatedTraceElement, final String prefix)
          throws IOException {
    List<StackTraceElement> trace = t.getStackTrace();
    writeTo(trace, to, detail, abbreviatedTraceElement, prefix);

    Throwable ourCause = t.getCause();
    List<Throwable> suppressed = t.getSuppressed();
    if (ourCause == null && suppressed.isEmpty()) {
      return;
    }
    Set<Throwable> dejaVu = new IdentityHashSet<Throwable>();
    dejaVu.add(t);
    // Print suppressed exceptions, if any
    for (Throwable se : suppressed) {
      printEnclosedStackTrace(se, to, trace, SUPPRESSED_CAPTION, prefix + "\t",
              dejaVu, detail, abbreviatedTraceElement);
    }
    // Print cause, if any
    if (ourCause != null) {
      printEnclosedStackTrace(ourCause, to, trace, CAUSE_CAPTION, prefix, dejaVu, detail, abbreviatedTraceElement);
    }
  }

  public static void writeTo(final List<StackTraceElement> trace, final Appendable to,
          final Throwables.PackageDetail detail,
          final boolean abbreviatedTraceElement, final String prefix)
          throws IOException {
    StackTraceElement prevElem = null;
    for (StackTraceElement traceElement : trace) {
      to.append(prefix);
      to.append("\tat ");
      writeTo(traceElement, prevElem, to, detail, abbreviatedTraceElement);
      to.append('\n');
      prevElem = traceElement;
    }
  }


  public static void writeTo(final StackTraceElement element, @Nullable final StackTraceElement previous,
          final Appendable to, final Throwables.PackageDetail detail,
          final boolean abbreviatedTraceElement)
          throws IOException {
    Method method = element.getMethod();
    String currClassName = method.getDeclaringClass();
    String prevClassName = previous == null ? null : previous.getMethod().getDeclaringClass();
    if (abbreviatedTraceElement) {
      if (currClassName.equals(prevClassName)) {
        to.append('^');
      } else {
        writeAbreviatedClassName(currClassName, to);
      }
    } else {
      to.append(currClassName);
    }
    to.append('.');
    to.append(method.getName());
    FileLocation location = element.getLocation();
    FileLocation prevLocation = previous == null ? null : previous.getLocation();
    String currFileName = location != null ? location.getFileName() : "";
    String prevFileName = prevLocation != null ? prevLocation.getFileName() : "";
    String fileName = currFileName;
    if (abbreviatedTraceElement && java.util.Objects.equals(currFileName, prevFileName)) {
      fileName = "^";
    }
    final int lineNumber = location != null ? location.getLineNumber() : -1;
    if (fileName.isEmpty()) {
      to.append("(Unknown Source)");
    } else if (lineNumber >= 0) {
      to.append('(').append(fileName).append(':')
              .append(Integer.toString(lineNumber)).append(')');
    } else {
      to.append('(').append(fileName).append(')');
    }
    if (detail == Throwables.PackageDetail.NONE) {
      return;
    }
    if (abbreviatedTraceElement && currClassName.equals(prevClassName)) {
      to.append("[^]");
      return;
    }
    PackageInfo presPackageInfo = previous == null ? null : previous.getPackageInfo();
    org.spf4j.base.avro.PackageInfo pInfo = element.getPackageInfo();
    if (abbreviatedTraceElement  && Objects.equals(pInfo, presPackageInfo)) {
      to.append("[^]");
      return;
    }
    if (!pInfo.getUrl().isEmpty() || !pInfo.getVersion().isEmpty()) {
      String jarSourceUrl = pInfo.getUrl();
      String version = pInfo.getVersion();
      to.append('[');
      if (!jarSourceUrl.isEmpty()) {
        if (detail == Throwables.PackageDetail.SHORT) {
          String url = jarSourceUrl;
          int lastIndexOf = url.lastIndexOf('/');
          if (lastIndexOf >= 0) {
            int lpos = url.length() - 1;
            if (lastIndexOf == lpos) {
              int prevSlPos = url.lastIndexOf('/', lpos - 1);
              if (prevSlPos < 0) {
                to.append(url);
              } else {
                to.append(url, prevSlPos + 1, url.length());
              }
            } else {
              to.append(url, lastIndexOf + 1, url.length());
            }
          } else {
            to.append(url);
          }
        } else {
          to.append(jarSourceUrl);
        }
      } else {
        to.append("na");
      }
      if (!version.isEmpty()) {
        to.append(':');
        to.append(version);
      }
      to.append(']');
    }
  }

  private static void printEnclosedStackTrace(final Throwable t, final Appendable s,
          final List<StackTraceElement> enclosingTrace,
          final String caption,
          final String prefix,
          final Set<Throwable> dejaVu,
          final Throwables.PackageDetail detail,
          final boolean abbreviatedTraceElement) throws IOException {
    if (dejaVu.contains(t)) {
      s.append("\t[CIRCULAR REFERENCE:");
      writeMessageString(s, t);
      s.append(']');
    } else {
      dejaVu.add(t);
      // Compute number of frames in common between this and enclosing trace
      List<StackTraceElement> trace = t.getStackTrace();
      int framesInCommon = commonFrames(trace, enclosingTrace);
      int m = trace.size() - framesInCommon;
      // Print our stack trace
      s.append(prefix).append(caption);
      writeMessageString(s, t);
      s.append('\n');
      StackTraceElement prev = null;
      for (int i = 0; i < m; i++) {
        s.append(prefix).append("\tat ");
        StackTraceElement ste = trace.get(i);
        writeTo(ste, prev, s, detail, abbreviatedTraceElement);
        s.append('\n');
        prev = ste;
      }
      if (framesInCommon != 0) {
        s.append(prefix).append("\t... ").append(Integer.toString(framesInCommon)).append(" more");
        s.append('\n');
      }

      // Print suppressed exceptions, if any
      for (Throwable se : t.getSuppressed()) {
        printEnclosedStackTrace(se, s, trace, SUPPRESSED_CAPTION, prefix + '\t',
                dejaVu, detail, abbreviatedTraceElement);
      }

      // Print cause, if any
      Throwable ourCause = t.getCause();
      if (ourCause != null) {
        printEnclosedStackTrace(ourCause, s, trace, CAUSE_CAPTION, prefix,
                dejaVu, detail, abbreviatedTraceElement);
      }
    }
  }

  public static int commonFrames(final List<StackTraceElement> trace,
          final List<StackTraceElement> enclosingTrace) {
    int from = trace.size() - 1;
    int m = from;
    int n = enclosingTrace.size() - 1;
    while (m >= 0 && n >= 0 && trace.get(m).equals(enclosingTrace.get(n))) {
      m--;
      n--;
    }
    return from - m;
  }

  public static void writeMessageString(final Appendable to, final Throwable t) throws IOException {
    to.append(t.getClass().getName());
    String message = t.getMessage();
    if (message != null) {
      to.append(':').append(message);
    }
  }

}
