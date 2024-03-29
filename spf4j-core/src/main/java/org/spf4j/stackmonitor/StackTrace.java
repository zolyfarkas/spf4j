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
package org.spf4j.stackmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;

/**
 *
 * @author zoly
 */
public final class StackTrace {

  public static final StackTraceElement[] EMPTY_STACK_TRACE
        = new StackTraceElement[0];

  private final StackTraceElement[] stackTrace;

  private final int relevantFramesStart;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public StackTrace(final StackTraceElement[] stackTrace, final int relevantFramesStart) {
    this.stackTrace = stackTrace;
    this.relevantFramesStart = relevantFramesStart;
  }

  public static StackTrace from(final StackTraceElement[] stackTrace, final int relevantFramesStart) {
    return new StackTrace(stackTrace, relevantFramesStart);
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public StackTraceElement[] getStackTrace() {
    return stackTrace;
  }

  public int getRelevantFramesStart() {
    return relevantFramesStart;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    return 53 * hash + Arrays.deepHashCode(this.stackTrace);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final StackTrace other = (StackTrace) obj;
    return org.spf4j.base.Arrays.deepEquals(this.stackTrace, other.stackTrace, relevantFramesStart);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (int i = relevantFramesStart; i < stackTrace.length; i++) {
      StackTraceElement elem = stackTrace[i];
      result.append(elem.getMethodName()).append('@').append(elem.getClassName()).append("->");
    }
    return result.toString();
  }

}
