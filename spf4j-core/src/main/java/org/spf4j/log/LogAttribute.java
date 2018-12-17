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

import java.util.concurrent.TimeUnit;
import org.spf4j.base.Pair;

/**
 * @author Zoltan Farkas
 */
public final class LogAttribute extends Pair<String, Object> {

  public LogAttribute(final String first, final Object second) {
    super(first, second);
  }

  public static LogAttribute of(final String val, final Object obj) {
    return new LogAttribute(val, obj);
  }

  public static LogAttribute traceId(final CharSequence id) {
    return new LogAttribute("trId", id);
  }

  public static LogAttribute origLevel(final Level level) {
    return new LogAttribute("origLevel", level);
  }

  public static LogAttribute log(final Slf4jLogRecord record) {
    return new LogAttribute("log", record);
  }

  public static LogAttribute execTimeMicros(final long time, final TimeUnit tu) {
    return new LogAttribute("execUs", tu.toMicros(time));
  }

  public static LogAttribute value(String what, final long value) {
    return new LogAttribute(what, value);
  }

}
