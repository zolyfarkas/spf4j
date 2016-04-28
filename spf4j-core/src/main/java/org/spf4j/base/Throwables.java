/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Reflections.PackageInfo;

/**
 * utility class for throwables.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Throwables {

  private Throwables() {
  }

  static final class Lazy {

    private static final Logger LOG = LoggerFactory.getLogger(Throwables.Lazy.class);
  }

  private static final int MAX_THROWABLE_CHAIN
          = Integer.getInteger("throwables.max.chain", 200);

  private static final Field CAUSE_FIELD;

  private static final Field SUPPRESSED_FIELD;

  static {
    CAUSE_FIELD = AccessController.doPrivileged(new PrivilegedAction<Field>() {
      @Override
      public Field run() {
        Field causeField;
        try {
          causeField = Throwable.class.getDeclaredField("cause");
        } catch (NoSuchFieldException | SecurityException ex) {
          throw new RuntimeException(ex);
        }
        causeField.setAccessible(true);
        return causeField;
      }
    });

    SUPPRESSED_FIELD = AccessController.doPrivileged(new PrivilegedAction<Field>() {
      @Override
      public Field run() {
        Field suppressedField;
        try {
          suppressedField = Throwable.class.getDeclaredField("suppressedExceptions");
        } catch (NoSuchFieldException | SecurityException ex) {
          Lazy.LOG.info("No access to suppressed Exceptions", ex);
          return null;
        }
        suppressedField.setAccessible(true);
        return suppressedField;
      }
    });
  }

  public static int getNrSuppressedExceptions(final Throwable t) {
    try {
      final List<Throwable> suppressedExceptions = (List<Throwable>) SUPPRESSED_FIELD.get(t);
      if (suppressedExceptions != null) {
        return suppressedExceptions.size();
      } else {
        return 0;
      }
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static int getNrRecursiveSuppressedExceptions(final Throwable t) {
    try {
      final List<Throwable> suppressedExceptions = (List<Throwable>) SUPPRESSED_FIELD.get(t);
      if (suppressedExceptions != null) {
        int count = 0;
        for (Throwable se : suppressedExceptions) {
          count += 1 + getNrRecursiveSuppressedExceptions(se);
        }
        return count;
      } else {
        return 0;
      }
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Throwable removeOldestSuppressedRecursive(final Throwable t) {
    try {
      final List<Throwable> suppressedExceptions = (List<Throwable>) SUPPRESSED_FIELD.get(t);
      if (suppressedExceptions != null && !suppressedExceptions.isEmpty()) {
        Throwable ex = suppressedExceptions.get(0);
        if (getNrSuppressedExceptions(ex) > 0) {
          return removeOldestSuppressedRecursive(ex);
        } else {
          return suppressedExceptions.remove(0);
        }
      } else {
        return null;
      }
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Throwable removeOldestSuppressed(final Throwable t) {
    try {
      final List<Throwable> suppressedExceptions = (List<Throwable>) SUPPRESSED_FIELD.get(t);
      if (suppressedExceptions != null && !suppressedExceptions.isEmpty()) {
        return suppressedExceptions.remove(0);
      } else {
        return null;
      }
    } catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void chain0(final Throwable t, final Throwable cause) {
    final Throwable rc = com.google.common.base.Throwables.getRootCause(t);
    setCause(rc, cause);
  }

  private static void setCause(final Throwable rc, @Nullable final Throwable cause) {
    try {
      AccessController.doPrivileged(new PrivilegedAction() {
        @Override
        public Object run() {
          try {
            CAUSE_FIELD.set(rc, cause);
          } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
          }
          return null; // nothing to return
        }
      });

    } catch (IllegalArgumentException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * This method will clone the exception t and will set a new root cause.
   *
   * @param <T>
   * @param t
   * @param newRootCause
   * @return
   */
  public static <T extends Throwable> T chain(final T t, final Throwable newRootCause) {
    int chainedExNr = com.google.common.base.Throwables.getCausalChain(t).size();
    if (chainedExNr >= MAX_THROWABLE_CHAIN) {
      Lazy.LOG.warn("Trimming exception", newRootCause);
      return t;
    }
    List<Throwable> newRootCauseChain = com.google.common.base.Throwables.getCausalChain(newRootCause);
    int newChainIdx = 0;
    final int size = newRootCauseChain.size();
    if (chainedExNr + size > MAX_THROWABLE_CHAIN) {
      newChainIdx = size - (MAX_THROWABLE_CHAIN - chainedExNr);
      Lazy.LOG.warn("Trimming exception at {} ", newChainIdx, newRootCause);
    }
    T result;
    try {
      result = Objects.clone(t);
    } catch (IOException ex) {
      result = t;
      Lazy.LOG.info("Unable to clone exception {}", t, ex);
    }
    chain0(result, newRootCauseChain.get(newChainIdx));
    return result;

  }

  public static void trimCausalChain(final Throwable t, final int maxSize) {
    List<Throwable> causalChain = com.google.common.base.Throwables.getCausalChain(t);
    if (causalChain.size() <= maxSize) {
      return;
    }
    setCause(causalChain.get(maxSize - 1), null);
  }

  /**
   * Functionality similar for java 1.7 Throwable.addSuppressed. 2 extra things happen:
   *
   * 1) limit to nr of exceptions suppressed. 2) Suppression does not mutate Exception, it clones it.
   *
   * @param <T>
   * @param t
   * @param suppressed
   * @return
   */
  @CheckReturnValue
  public static <T extends Throwable> T suppress(@Nonnull final T t, @Nonnull final Throwable suppressed) {
    T clone;
    try {
      clone = Objects.clone(t);
    } catch (IOException ex) {
      clone = t;
      Lazy.LOG.info("Unable to clone exception", t);
    }
    clone.addSuppressed(suppressed);
    while (getNrRecursiveSuppressedExceptions(clone) > MAX_THROWABLE_CHAIN) {
      if (removeOldestSuppressedRecursive(clone) == null) {
        throw new IllegalArgumentException("Impossible state for " + clone);
      }
    }
    return clone;
  }

  /**
   * Utility to get suppressed exceptions.
   *
   * In java 1.7 it will return t.getSuppressed() + in case it is Iterable<Throwable> any other linked exceptions (see
   * SQLException)
   *
   * java 1.6 behavior is deprecated.
   *
   * @param t
   * @return
   */
  public static Throwable[] getSuppressed(final Throwable t) {
    if (t instanceof Iterable) {
      // see SQLException
      List<Throwable> suppressed = new ArrayList<>(java.util.Arrays.asList(t.getSuppressed()));
      Set<Throwable> ignore = new HashSet<>();
      ignore.addAll(com.google.common.base.Throwables.getCausalChain(t));
      Iterator it = ((Iterable) t).iterator();
      while (it.hasNext()) {
        Object next = it.next();
        if (next instanceof Throwable) {
          if (ignore.contains((Throwable) next)) {
            continue;
          }
          suppressed.add((Throwable) next);
          ignore.addAll(com.google.common.base.Throwables.getCausalChain((Throwable) next));
        } else {
          break;
        }
      }
      return suppressed.toArray(new Throwable[suppressed.size()]);
    } else {
      return t.getSuppressed();
    }

  }

  public static void writeTo(final StackTraceElement element, final Appendable to, final Detail detail)
          throws IOException {
    to.append(element.getClassName());
    to.append('.');
    to.append(element.getMethodName());
    final String fileName = element.getFileName();
    final int lineNumber = element.getLineNumber();
    if (element.isNativeMethod()) {
      to.append("(Native Method)");
    } else if (fileName != null && lineNumber >= 0) {
      to.append('(').append(fileName).append(':')
              .append(Integer.toString(lineNumber)).append(')');
    } else if (fileName != null) {
      to.append('(').append(fileName).append(')');
    } else {
      to.append("(Unknown Source)");
    }
    if (detail == Detail.NONE) {
      return;
    }
    PackageInfo pInfo = Reflections.getPackageInfo(element.getClassName());
    if (pInfo.hasInfo()) {
      URL jarSourceUrl = pInfo.getUrl();
      String version = pInfo.getVersion();
      to.append('[');
      if (jarSourceUrl != null) {
        if (detail == Detail.SHORT_PACKAGE || detail == Detail.STANDARD) {
          String url = jarSourceUrl.toString();
          int lastIndexOf = url.lastIndexOf('/');
          if (lastIndexOf >= 0) {
            to.append(url, lastIndexOf + 1, url.length());
          } else {
            to.append(url);
          }
        } else {
          to.append(jarSourceUrl.toString());
        }
      } else {
        to.append("na");
      }
      if (version != null) {
        to.append(':');
        to.append(version);
      }
      to.append(']');
    }
  }

  public enum Detail {
    NONE,
    STANDARD, // equivalent with SHORT_PACKAGE
    SHORT_PACKAGE,
    LONG_PACKAGE
  }

  private static final Detail DEFAULT_DETAIL
          = Detail.valueOf(System.getProperty("throwable.detail", "SHORT_PACKAGE"));

  public static String toString(final Throwable t) {
    return toString(t, DEFAULT_DETAIL);
  }

  public static String toString(final Throwable t, final Detail detail) {
    StringBuilder sb = toStringBuilder(t, detail);
    return sb.toString();
  }

  public static StringBuilder toStringBuilder(final Throwable t, final Detail detail) {
    StringBuilder sb = new StringBuilder(1024);
    try {
      writeTo(t, sb, detail);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return sb;
  }

  @SuppressFBWarnings({"OCP_OVERLY_CONCRETE_PARAMETER", "NOS_NON_OWNED_SYNCHRONIZATION"})
  // I don't want this to throw a checked ex though... + I really want the coarse sync!
  public static void writeTo(@Nonnull final Throwable t, @Nonnull final PrintStream to,
          @Nonnull final Detail detail) {
    try {
      synchronized (to) {
        writeTo(t, (Appendable) to, detail);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void writeTo(final Throwable t, final Appendable to, final Detail detail) throws IOException {

    Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
    dejaVu.add(t);

    toString(to, t);
    to.append('\n');
    StackTraceElement[] trace = t.getStackTrace();

    writeTo(trace, to, detail);

    // Print suppressed exceptions, if any
    for (Throwable se : getSuppressed(t)) {
      printEnclosedStackTrace(se, to, trace, SUPPRESSED_CAPTION, "\t", dejaVu, detail);
    }

    Throwable ourCause = t.getCause();

    // Print cause, if any
    if (ourCause != null) {
      printEnclosedStackTrace(ourCause, to, trace, CAUSE_CAPTION, "", dejaVu, detail);
    }

  }

  public static void toString(final Appendable to, final Throwable t) throws IOException {
    // Print our stack trace
    to.append(t.getClass().getName());
    String message = t.getMessage();
    if (message != null) {
      to.append(':').append(message);
    }
  }

  public static void writeTo(final StackTraceElement[] trace, final Appendable to, final Detail detail)
          throws IOException {
    for (StackTraceElement traceElement : trace) {
      to.append("\tat ");
      writeTo(traceElement, to, detail);
      to.append('\n');
    }
  }

  public static int commonFrames(final StackTraceElement[] trace, final StackTraceElement[] enclosingTrace) {
    int m = trace.length - 1;
    int n = enclosingTrace.length - 1;
    while (m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n])) {
      m--;
      n--;
    }
    return trace.length - 1 - m;
  }

  private static void printEnclosedStackTrace(final Throwable t, final Appendable s,
          final StackTraceElement[] enclosingTrace,
          final String caption,
          final String prefix,
          final Set<Throwable> dejaVu,
          final Detail detail) throws IOException {
    if (dejaVu.contains(t)) {
      s.append("\t[CIRCULAR REFERENCE:");
      toString(s, t);
      s.append(']');
    } else {
      dejaVu.add(t);
      // Compute number of frames in common between this and enclosing trace
      StackTraceElement[] trace = t.getStackTrace();
      int framesInCommon = commonFrames(trace, enclosingTrace);
      int m = trace.length - framesInCommon;
      // Print our stack trace
      s.append(prefix).append(caption);
      toString(s, t);
      s.append('\n');
      for (int i = 0; i < m; i++) {
        s.append(prefix).append("\tat ");
        writeTo(trace[i], s, detail);
        s.append('\n');
      }
      if (framesInCommon != 0) {
        s.append(prefix).append("\t... ").append(Integer.toString(framesInCommon)).append(" more");
        s.append('\n');
      }

      // Print suppressed exceptions, if any
      for (Throwable se : getSuppressed(t)) {
        printEnclosedStackTrace(se, s, trace, SUPPRESSED_CAPTION, prefix + '\t', dejaVu, detail);
      }

      // Print cause, if any
      Throwable ourCause = t.getCause();
      if (ourCause != null) {
        printEnclosedStackTrace(ourCause, s, trace, CAUSE_CAPTION, prefix, dejaVu, detail);
      }
    }
  }

  /**
   * Caption for labeling suppressed exception stack traces
   */
  public static final String SUPPRESSED_CAPTION = "Suppressed: ";
  /**
   * Caption for labeling causative exception stack traces
   */
  public static final String CAUSE_CAPTION = "Caused by: ";

//    public static final class RepeatingStack {
//        private final int count;
//        private final List<Either<StackTraceElement, RepeatingStack>> stack;
//
//        public RepeatingStack(final int count, final List<Either<StackTraceElement, RepeatingStack>> stack) {
//            this.count = count;
//            this.stack = stack;
//        }
//    }
//
//
//    public static List<Either<StackTraceElement, RepeatingStack>> compress(final StackTraceElement [] trace) {
//        Map<StackTraceElement, Integer> elementLocation = new HashMap<>(trace.length);
//        for (int i = 0; i < trace.length; i++) {
//            StackTraceElement elem = trace[i];
//            if (elementLocation.containsKey(elem)) {
//                // potentially repeating stack trace
//                int j = elementLocation.get(elem);
//                boolean repeating = true;
//                for (int k = j + 1; k < i; k++) {
//                    if (!trace[k].equals(trace[i + k])) {
//                        repeating = false;
//                        break;
//                    }
//                }
//                if (repeating) {
//                    for (int k = j; k < i; k++) {
//
//                    }
//                }
//
//            } else {
//                elementLocation.put(elem, i);
//            }
//        }
//    }
}
