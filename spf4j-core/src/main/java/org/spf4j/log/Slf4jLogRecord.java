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
package org.spf4j.log;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Marker;
import org.spf4j.base.StackSamples;
import org.spf4j.base.avro.Converters;
import static org.spf4j.base.avro.Converters.convert;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.io.ObjectAppenderSupplier;

/**
 * @author Zoltan Farkas
 */
public interface Slf4jLogRecord {

  /**
   * @return all slf4j log arguments, getNrMessageArguments() message arguments followed by additional payload
   * like exceptions, or other data.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP")
  @Nonnull
  Object[] getArguments();

  /**
   * @return all extra arguments. (arguments that are not parameters for the message)
   */
  @Nonnull
  Object[] getExtraArgumentsRaw();

  /**
   * @return all non Throwable extra arguments.
   */
  @Nonnull
  Object[] getExtraArguments();

  /**
   * @return Throwable from extra arguments. If multiple, will return first will all others as suppressed.
   */
  @Nullable
  Throwable getExtraThrowable();

  Level getLevel();

  String getLoggerName();

  @Nullable
  Marker getMarker();

  @Nonnull
  String getMessage();

  String getMessageFormat();

  int getNrMessageArguments();

  String getThreadName();

  long getTimeStamp();

  default Instant getTimeStampInstant() {
    return Instant.ofEpochMilli(getTimeStamp());
  }

  /**
   * Indicates that this log record has been sent to the logging backend to persist.
   *
   * @return
   */
  boolean isLogged();

  void setIsLogged();

  void attach(Object obj);

  Set<Object> getAttachments();

  boolean hasAttachment(Object obj);

  static int compareByTimestamp(Slf4jLogRecord a, Slf4jLogRecord b) {
    long timeDiff = a.getTimeStamp() - b.getTimeStamp();
    if (timeDiff > 0) {
      return 1;
    } else if (timeDiff < 0) {
      return -1;
    } else {
      return 0;
    }
  }

  @SuppressFBWarnings("WOC_WRITE_ONLY_COLLECTION_LOCAL")
  default LogRecord toLogRecord(final String origin, final String ptraceId) {
    java.lang.Throwable extraThrowable = this.getExtraThrowable();
    Marker marker = this.getMarker();
    Object[] extraArguments = this.getExtraArguments();
    Map<String, Object> attribs = null;
    List<Object> xArgs;
    String traceId = ptraceId;
    List<StackSampleElement> profiles = Collections.EMPTY_LIST;
    if (extraArguments.length == 0) {
      xArgs = Collections.emptyList();
    } else {
      int nrAttribs = 0;
      for (Object obj : extraArguments) {
        if (obj instanceof LogAttribute) {
          LogAttribute la = (LogAttribute) obj;
          String name = la.getName();
          switch (name) {
            case LogAttribute.ID_ATTR_NAME:
              traceId = la.getValue().toString();
              break;
            case LogAttribute.PROFILE_SAMPLES_ATTR_NAME:
              profiles = Converters.convert((StackSamples) la.getValue());
              break;
            default:
              // nothiing to do.
          }
          nrAttribs++;
        }
      }
      if (nrAttribs == 0) {
        xArgs = Arrays.asList(extraArguments);
      } else {
        if (nrAttribs == extraArguments.length) {
          xArgs = Collections.EMPTY_LIST;
        } else {
          xArgs = new ArrayList<>(extraArguments.length - nrAttribs);
        }
        attribs = Maps.newHashMapWithExpectedSize(nrAttribs + (marker == null ? 0 : 1));
        for (Object obj : extraArguments) {
          if (obj instanceof LogAttribute) {
            LogAttribute la = (LogAttribute) obj;
            String aName = la.getName();
            switch (aName) {
              case LogAttribute.ID_ATTR_NAME:
              case LogAttribute.PROFILE_SAMPLES_ATTR_NAME:
                break;
              default:
                attribs.put(aName, la.getValue());
            }
          } else {
            xArgs.add(obj);
          }
        }
        if (marker != null) {
          attribs.put(marker.getName(), marker);
        }
      }
    }
    int nrMsgArgs = this.getNrMessageArguments();
    List<String> messageArgs;
    if (nrMsgArgs == 0) {
        messageArgs = Collections.emptyList();
    } else {
        Object[] args = this.getArguments();
        String[] ma = new String[nrMsgArgs];
        for (int i = 0; i < nrMsgArgs; i++) {
          Object arg = args[i];
          if (arg == null) {
            ma[i] = "null";
          } else {
            ma[i] = ObjectAppenderSupplier.TO_STRINGER.get(arg.getClass()).toString(arg);
          }
        }
        messageArgs = Arrays.asList(ma);
    }
    return new LogRecord(origin, traceId, this.getLevel().getAvroLevel(),
            Instant.ofEpochMilli(this.getTimeStamp()),
            this.getLoggerName(), this.getThreadName(), this.getMessageFormat(),
            messageArgs, xArgs,
            attribs == null ? Collections.emptyMap() : attribs,
            extraThrowable == null ? null : convert(extraThrowable), profiles);
  }

}
