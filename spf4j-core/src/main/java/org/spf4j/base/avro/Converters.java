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

import gnu.trove.map.TMap;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Handler;
import org.spf4j.ds.IdentityHashSet;
import org.spf4j.log.Slf4jLogRecord;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class Converters {

  public static final Method ROOT = new Method(ManagementFactory.getRuntimeMXBean().getName(), "ROOT");

  private Converters() { }

  public static StackTraceElement convert(final java.lang.StackTraceElement stackTrace) {
    String className = stackTrace.getClassName();
    String fileName = stackTrace.getFileName();
    return new StackTraceElement(new Method(className, stackTrace.getMethodName()),
            fileName == null ? null :  new FileLocation(fileName, stackTrace.getLineNumber(), -1),
            org.spf4j.base.PackageInfo.getPackageInfo(className));
  }

  public static List<StackTraceElement> convert(final java.lang.StackTraceElement[] stackTraces) {
    int l = stackTraces.length;
    if (l == 0) {
      return Collections.EMPTY_LIST;
    }
    List<StackTraceElement> result = new ArrayList<>(l);
    for (java.lang.StackTraceElement st : stackTraces) {
      result.add(convert(st));
    }
    return result;
  }

  public static List<Throwable> convert(final java.lang.Throwable[] throwables, final Set<java.lang.Throwable> seen) {
    int l = throwables.length;
    if (l == 0) {
      return Collections.EMPTY_LIST;
    }
    List<Throwable> result = new ArrayList<>(l);
    for (java.lang.Throwable t : throwables) {
      result.add(convert(t, seen));
    }
    return result;
  }

  public static Throwable convert(final java.lang.Throwable throwable) {
    return convert(throwable, new IdentityHashSet<>(4));
  }

  public static Throwable convert(final java.lang.Throwable throwable,
          final Set<java.lang.Throwable> seen) {
    if (seen.contains(throwable)) {
      return new Throwable(throwable.getClass().getName(),
              "CIRCULAR REFERENCE:" + throwable.getMessage(),
              Collections.EMPTY_LIST, null, Collections.EMPTY_LIST);
    } else {
      seen.add(throwable);
    }
    String message = throwable.getMessage();
    if (throwable instanceof RemoteException) {
        return new Throwable(throwable.getClass().getName(),
                message == null ? "" : message, convert(throwable.getStackTrace()),
                ((RemoteException) throwable).getRemoteCause(),
                convert(throwable.getSuppressed(), seen));
    }
    java.lang.Throwable cause = throwable.getCause();
    return new Throwable(throwable.getClass().getName(),
            message == null ? "" : message,
            convert(throwable.getStackTrace()),
            cause == null ? null : convert(cause, seen),
            convert(throwable.getSuppressed(), seen));
  }

  public static RemoteException convert(final String source, final Throwable throwable) {
    return new RemoteException(source, throwable);
  }


  public static List<LogRecord> convert(final String origin, final String traceId,
          final List<Slf4jLogRecord> logRecords) {
    List<LogRecord> result = new ArrayList<>(logRecords.size());
    for (Slf4jLogRecord log : logRecords) {
      result.add(log.toLogRecord(origin, traceId));
    }
    return result;
  }


  public static <E extends Exception> int convert(final Method method, final org.spf4j.base.StackSamples node,
          final int parentId, final int id,
          final Handler<StackSampleElement, E> handler) throws E {

    final Deque<TraversalNode> dq = new ArrayDeque<>();
    dq.addLast(new TraversalNode(method, node, parentId));
    int nid = id;
    while (!dq.isEmpty()) {
      TraversalNode first = dq.removeFirst();
      org.spf4j.base.StackSamples n = first.getNode();
      StackSampleElement sample = new StackSampleElement(nid, first.getParentId(),
              n.getSampleCount(), first.getMethod());
      final TMap<Method, ? extends org.spf4j.base.StackSamples> subNodes = n.getSubNodes();
      final int pid = nid;
      if (subNodes != null) {
        subNodes.forEachEntry((a, b) -> {
          dq.addLast(new TraversalNode(a, b, pid));
          return true;
        });
      }
      handler.handle(sample, parentId);
      nid++;
    }
    return nid;
  }


  private static final class TraversalNode {

    private final Method method;
    private final org.spf4j.base.StackSamples node;
    private final int parentId;

    TraversalNode(final Method method, final org.spf4j.base.StackSamples node, final int parentId) {
      this.method = method;
      this.node = node;
      this.parentId = parentId;
    }

    public Method getMethod() {
      return method;
    }

    public org.spf4j.base.StackSamples getNode() {
      return node;
    }

    public int getParentId() {
      return parentId;
    }

    @Override
    public String toString() {
      return "TraversalNode{" + "method=" + method + ", node=" + node + ", parentId=" + parentId + '}';
    }

  }

}
