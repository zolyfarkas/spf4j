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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.set.hash.THashSet;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.net.ssl.SSLException;
import org.spf4j.ds.IdentityHashSet;

/**
 * utility class for throwables.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Throwables {

  /**
   * Caption for labeling suppressed exception stack traces
   */
  public static final String SUPPRESSED_CAPTION = "Suppressed: ";
  /**
   * Caption for labeling causative exception stack traces
   */
  public static final String CAUSE_CAPTION = "Caused by: ";

  public static final int MAX_THROWABLE_CHAIN
          = Integer.getInteger("spf4j.throwables.defaultMaxSuppressChain", 100);

  private static final Field CAUSE_FIELD;

  private static final Field SUPPRESSED_FIELD;

  static {
    CAUSE_FIELD = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
      Field causeField;
      try {
        causeField = Throwable.class.getDeclaredField("cause");
      } catch (NoSuchFieldException | SecurityException ex) {
        throw new ExceptionInInitializerError(ex);
      }
      causeField.setAccessible(true);
      return causeField;
    });

    SUPPRESSED_FIELD = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
      Field suppressedField;
      try {
        suppressedField = Throwable.class.getDeclaredField("suppressedExceptions");
      } catch (NoSuchFieldException | SecurityException ex) {
        throw new ExceptionInInitializerError(ex);
      }
      suppressedField.setAccessible(true);
      return suppressedField;
    });
  }

  private static final PackageDetail DEFAULT_PACKAGE_DETAIL
          = PackageDetail.valueOf(System.getProperty("spf4j.throwables.defaultStackTracePackageDetail", "SHORT"));


  private static final boolean DEFAULT_TRACE_ELEMENT_ABBREVIATION
          = Boolean.parseBoolean(System.getProperty("spf4j.throwables.defaultStackTraceAbbreviation", "true"));


  private static volatile Predicate<Throwable> nonRecoverableClassificationPredicate = new Predicate<Throwable>() {
    @Override
    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public boolean test(final Throwable t) {

      if (t instanceof Error && !(t instanceof StackOverflowError)
              && !(t.getClass().getName().endsWith("TokenMgrError"))) {
        return true;
      }
      if (t instanceof IOException) {
        String message = t.getMessage();
        if (message != null && message.contains("Too many open files")) {
          return true;
        }
      }
      return false;
    }
  };


  private static volatile Predicate<Throwable> isRetryablePredicate = new Predicate<Throwable>() {

    /**
     * A default predicate that will return true if a exception is retriable...
     * @param t
     * @return
     */
    @Override
    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public boolean test(final Throwable t) {
      // non recoverables are not retryable.
      if (Throwables.containsNonRecoverable(t)) {
        return false;
      }
      // Root Cause
      Throwable rootCause = com.google.common.base.Throwables.getRootCause(t);
      if (rootCause instanceof SSLException) {
        return false;
      }
      if (rootCause instanceof RuntimeException) {
        String name = rootCause.getClass().getName();
        if (name.contains("NonTransient") || !name.contains("Transient")) {
          return false;
        }
      }
      // check causal chaing
      Throwable e = Throwables.firstCause(t,
              (ex) -> {
                String exClassName = ex.getClass().getName();
                return (ex instanceof SQLTransientException
              || ex instanceof SQLRecoverableException
              || (ex instanceof IOException && !exClassName.contains("Json"))
              || ex instanceof TimeoutException
              || (exClassName.contains("Transient")
                        && !exClassName.contains("NonTransient")));
                        });
      return e != null;
    }
  };


  private Throwables() {
  }

  /**
   * figure out if a Exception is retry-able or not.
   * If while executing a operation a exception is returned, that exception is retryable if retrying the operation
   * can potentially succeed.
   * @param value
   * @return
   */
  public static boolean isRetryable(final Exception value) {
    return isRetryablePredicate.test(value);
  }

  public static Predicate<Throwable> getIsRetryablePredicate() {
    return isRetryablePredicate;
  }

  public static void setIsRetryablePredicate(final Predicate<Throwable> isRetryablePredicate) {
    Throwables.isRetryablePredicate = isRetryablePredicate;
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

  @Nullable
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

  @Nullable
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
    return chain(t, newRootCause, MAX_THROWABLE_CHAIN);
  }

  public static final class TrimmedException extends Exception {

    public TrimmedException(final String message) {
      super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  }


  public static <T extends Throwable> T chain(final T t, final Throwable newRootCause, final int maxChained) {
    int chainedExNr = com.google.common.base.Throwables.getCausalChain(t).size();
    if (chainedExNr >= maxChained) {
      T res = clone0(t);
      res.addSuppressed(new TrimmedException("Max chained excetions exceeded " + maxChained));
      return res;
    }
    List<Throwable> newRootCauseChain = com.google.common.base.Throwables.getCausalChain(newRootCause);
    int newChainIdx = 0;
    final int size = newRootCauseChain.size();
    if (chainedExNr + size > maxChained) {
      newChainIdx = size - (maxChained - chainedExNr);
      t.addSuppressed(new TrimmedException("Trimming exception at " + newChainIdx));
    }
    T result = clone0(t);
    chain0(result, newRootCauseChain.get(newChainIdx));
    return result;

  }

  private static <T extends Throwable> T clone0(final T t) {
    T result;
    try {
      result = Objects.clone(t);
    } catch (RuntimeException ex) {
      result = t;
      t.addSuppressed(new TrimmedException("Unable to clone exception " + t));
    }
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
    return suppress(t, suppressed, MAX_THROWABLE_CHAIN);
  }

  @CheckReturnValue
  public static <T extends Throwable> T suppress(@Nonnull final T t, @Nonnull final Throwable suppressed,
          final int maxSuppressed) {
    T clone;
    try {
      clone = Objects.clone(t);
    } catch (RuntimeException ex) {
      t.addSuppressed(ex);
      clone = t;
    }
    clone.addSuppressed(suppressed);
    while (getNrRecursiveSuppressedExceptions(clone) > maxSuppressed) {
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

  public static void writeTo(final StackTraceElement element, @Nullable final StackTraceElement previous,
          final Appendable to, final PackageDetail detail,
          final boolean abbreviatedTraceElement)
          throws IOException {
    String currClassName = element.getClassName();
    String prevClassName = previous == null ? null : previous.getClassName();
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
    to.append(element.getMethodName());
    String currFileName = element.getFileName();
    String fileName = currFileName;
    if (abbreviatedTraceElement && java.util.Objects.equals(currFileName,
            previous == null ? null : previous.getFileName())) {
      fileName = "^";
    }
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
    if (detail == PackageDetail.NONE) {
      return;
    }
    if (abbreviatedTraceElement && currClassName.equals(prevClassName)) {
      to.append("[^]");
      return;
    }
    PackageInfo pInfo = PackageInfo.getPackageInfo(currClassName);
    if (abbreviatedTraceElement && prevClassName != null && pInfo.equals(PackageInfo.getPackageInfo(prevClassName))) {
      to.append("[^]");
      return;
    }
    if (pInfo.hasInfo()) {
      String jarSourceUrl = pInfo.getUrl();
      String version = pInfo.getVersion();
      to.append('[');
      if (jarSourceUrl != null) {
        if (detail == PackageDetail.SHORT) {
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
      if (version != null) {
        to.append(':');
        to.append(version);
      }
      to.append(']');
    }
  }

  /**
   * enum describing the PackageDetail level to be logged in the stack trace.
   */
  public enum PackageDetail {
    /**
     * No jar info or version info.
     */
    NONE,
    /**
     * jar file name + manifest version.
     */
    SHORT,
    /**
     * complete jar path + manifest version.
     */
    LONG

  }

  public static String toString(final Throwable t) {
    return toString(t, DEFAULT_PACKAGE_DETAIL);
  }

  public static String toString(final Throwable t, final PackageDetail detail) {
    return toString(t, detail, DEFAULT_TRACE_ELEMENT_ABBREVIATION);
  }

  public static String toString(final Throwable t, final PackageDetail detail, final boolean abbreviatedTraceElement) {
    StringBuilder sb = toStringBuilder(t, detail, abbreviatedTraceElement);
    return sb.toString();
  }

  public static StringBuilder toStringBuilder(final Throwable t, final PackageDetail detail) {
    return toStringBuilder(t, detail, DEFAULT_TRACE_ELEMENT_ABBREVIATION);
  }

  public static StringBuilder toStringBuilder(final Throwable t, final PackageDetail detail,
          final boolean abbreviatedTraceElement) {
    StringBuilder sb = new StringBuilder(1024);
    try {
      writeTo(t, sb, detail, abbreviatedTraceElement);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return sb;
  }

  public static void writeTo(@Nonnull final Throwable t, @Nonnull final PrintStream to,
          @Nonnull final PackageDetail detail) {
    writeTo(t, to, detail, DEFAULT_TRACE_ELEMENT_ABBREVIATION);
  }

  @SuppressFBWarnings({"OCP_OVERLY_CONCRETE_PARAMETER"}) // on purpose :-)
  public static void writeTo(@Nonnull final Throwable t, @Nonnull final PrintStream to,
          @Nonnull final PackageDetail detail, final boolean abbreviatedTraceElement) {
    StringBuilder sb = new StringBuilder(1024);
    try {
      writeTo(t, sb, detail, abbreviatedTraceElement);
      to.append(sb);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void writeTo(final Throwable t, final Appendable to, final PackageDetail detail) throws IOException {
    writeTo(t, to, detail, DEFAULT_TRACE_ELEMENT_ABBREVIATION);
  }

  public static void writeTo(final Throwable t, final Appendable to, final PackageDetail detail,
          final boolean abbreviatedTraceElement) throws IOException {

    Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
    dejaVu.add(t);

    writeMessageString(to, t);
    to.append('\n');
    StackTraceElement[] trace = t.getStackTrace();

    writeTo(trace, to, detail, abbreviatedTraceElement);

    // Print suppressed exceptions, if any
    for (Throwable se : getSuppressed(t)) {
      printEnclosedStackTrace(se, to, trace, SUPPRESSED_CAPTION, "\t", dejaVu, detail, abbreviatedTraceElement);
    }

    Throwable ourCause = t.getCause();

    // Print cause, if any
    if (ourCause != null) {
      printEnclosedStackTrace(ourCause, to, trace, CAUSE_CAPTION, "", dejaVu, detail, abbreviatedTraceElement);
    }

  }

  public static void writeMessageString(final Appendable to, final Throwable t) throws IOException {
    to.append(t.getClass().getName());
    String message = t.getMessage();
    if (message != null) {
      to.append(':').append(message);
    }
  }

  public static void writeTo(final StackTraceElement[] trace, final Appendable to, final PackageDetail detail,
          final boolean abbreviatedTraceElement)
          throws IOException {

    StackTraceElement prevElem = null;
    for (StackTraceElement traceElement : trace) {
      to.append("\tat ");
      writeTo(traceElement, prevElem, to, detail, abbreviatedTraceElement);
      to.append('\n');
      prevElem = traceElement;
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
          final PackageDetail detail,
          final boolean abbreviatedTraceElement) throws IOException {
    if (dejaVu.contains(t)) {
      s.append("\t[CIRCULAR REFERENCE:");
      writeMessageString(s, t);
      s.append(']');
    } else {
      dejaVu.add(t);
      // Compute number of frames in common between this and enclosing trace
      StackTraceElement[] trace = t.getStackTrace();
      int framesInCommon = commonFrames(trace, enclosingTrace);
      int m = trace.length - framesInCommon;
      // Print our stack trace
      s.append(prefix).append(caption);
      writeMessageString(s, t);
      s.append('\n');
      StackTraceElement prev = null;
      for (int i = 0; i < m; i++) {
        s.append(prefix).append("\tat ");
        StackTraceElement ste = trace[i];
        writeTo(ste, prev, s, detail, abbreviatedTraceElement);
        s.append('\n');
        prev = ste;
      }
      if (framesInCommon != 0) {
        s.append(prefix).append("\t... ").append(Integer.toString(framesInCommon)).append(" more");
        s.append('\n');
      }

      // Print suppressed exceptions, if any
      for (Throwable se : getSuppressed(t)) {
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

  /**
   * Is this Throwable a JVM non-recoverable exception. (Oom, VMError, etc...)
   * @param t
   * @return
   */
  public static boolean isNonRecoverable(@Nonnull final Throwable t) {
    return nonRecoverableClassificationPredicate.test(t);
  }

  /**
   * Does this Throwable contain a JVM non-recoverable exception. (Oom, VMError, etc...)
   * @param t
   * @return
   */
  public static boolean containsNonRecoverable(@Nonnull final Throwable t) {
    return contains(t, nonRecoverableClassificationPredicate);
  }

  /**
   * checks in the throwable + children (both causal and suppressed) contain a throwable that
   * respects the Predicate.
   * @param t the throwable
   * @param predicate the predicate
   * @return true if a Throwable matching the predicate is found.
   */
  public static boolean contains(@Nonnull final Throwable t, final Predicate<Throwable> predicate) {
    return first(t, predicate) != null;
  }


  /**
   * return first Exception in the causal chain Assignable to clasz.
   * @param <T>
   * @param t
   * @param clasz
   * @return
   */
  @Nullable
  @CheckReturnValue
  public static <T extends Throwable> T first(@Nonnull final Throwable t, final Class<T> clasz) {
    return (T) first(t, (Throwable th) -> clasz.isAssignableFrom(th.getClass()));
  }

  /**
   * Returns the first Throwable that matches the predicate in the causal and suppressed chain.
   * @param t the Throwable
   * @param predicate the Predicate
   * @return the Throwable the first matches the predicate or null is none matches.
   */
  @Nullable
  @CheckReturnValue
  public static Throwable first(@Nonnull final Throwable t, final Predicate<Throwable> predicate) {
    ArrayDeque<Throwable> toScan =  new ArrayDeque<>();
    toScan.addFirst(t);
    Throwable th;
    THashSet<Throwable> seen = new IdentityHashSet<>();
    while ((th = toScan.pollFirst()) != null) {
      if (seen.contains(th)) {
        continue;
      }
      if (predicate.test(th)) {
        return th;
      } else {
        Throwable cause = th.getCause();
        if (cause != null) {
          toScan.addFirst(cause);
        }
        for (Throwable supp : th.getSuppressed()) {
          toScan.addLast(supp);
        }
      }
      seen.add(th);
    }
    return null;
  }


  /**
   * Returns first Throwable in the causality chain that is matching the provided predicate.
   * @param throwable the Throwable to go through.
   * @param predicate the predicate to apply
   * @return the first Throwable from the chain that the predicate matches.
   */

  @Nullable
  @CheckReturnValue
  public static Throwable firstCause(@Nonnull final Throwable throwable, final Predicate<Throwable> predicate) {
    Throwable t = throwable;
    do {
      if (predicate.test(t)) {
        return t;
      }
      t = t.getCause();
    } while (t != null);
    return null;
  }


  public static Predicate<Throwable> getNonRecoverablePredicate() {
    return nonRecoverableClassificationPredicate;
  }

  /**
   * Overwrite the default non-recoverable predicate.
   * @param predicate
   */
  public static void setNonRecoverablePredicate(final Predicate<Throwable> predicate) {
    Throwables.nonRecoverableClassificationPredicate = predicate;
  }

  public static void writeAbreviatedClassName(final String className, final Appendable writeTo) throws IOException {
    int ldIdx = className.lastIndexOf('.');
    if (ldIdx < 0) {
      writeTo.append(className);
      return;
    }
    boolean isPreviousDot = true;
    for (int i = 0; i < ldIdx; i++) {
      char c = className.charAt(i);
      boolean isCurrentCharDot = c == '.';
      if (isPreviousDot || isCurrentCharDot) {
        writeTo.append(c);
      }
      isPreviousDot = isCurrentCharDot;
    }
    writeTo.append(className, ldIdx, className.length());
  }

  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  public static void throwException(final Exception ex) throws IOException, InterruptedException,
          ExecutionException, TimeoutException {
    if (ex instanceof IOException) {
      throw (IOException) ex;
    } else if (ex instanceof InterruptedException) {
      throw (InterruptedException) ex;
    } else if (ex instanceof ExecutionException) {
      throw (ExecutionException) ex;
    } else if (ex instanceof TimeoutException) {
      throw (TimeoutException) ex;
    } else {
      throw new ExecutionException(ex);
    }
  }


}
