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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Marker;
import org.spf4j.base.CharSequences;
import org.spf4j.base.Json;
import org.spf4j.base.JsonWriteable;
import org.spf4j.base.Pair;
import org.spf4j.io.AppendableWriter;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({ "PIS_POSSIBLE_INCOMPLETE_SERIALIZATION", "SE_NO_SUITABLE_CONSTRUCTOR" })
public final class LogAttribute<T> extends Pair<String, T>
  implements JsonWriteable, Marker {

  private static final long serialVersionUID = 1L;

  public LogAttribute(final String first, final T second) {
    super(first, second);
  }

  public String getName() {
    return first;
  }

  public static <T> LogAttribute<T> of(final String val, final T obj) {
    return new LogAttribute(val, obj);
  }

  public static LogAttribute<CharSequence> traceId(final CharSequence id) {
    return new LogAttribute("trId", id);
  }

  public static LogAttribute<Level> origLevel(final Level level) {
    return new LogAttribute("origLevel", level);
  }

  public static LogAttribute<Level> origLoggerName(final String loggerName) {
    return new LogAttribute("origLogger", loggerName);
  }

  public static LogAttribute<Level> origTimeStamp(final Instant instant) {
    return new LogAttribute("origTs", instant);
  }

  public static LogAttribute<Level> origTimeStamp(final long millisSinceEpoch) {
    return new LogAttribute("origTs", Instant.ofEpochMilli(millisSinceEpoch));
  }

  public static LogAttribute<Slf4jLogRecord> log(final Slf4jLogRecord record) {
    return new LogAttribute("log", record);
  }

  public static LogAttribute<Long> execTimeMicros(final long time, final TimeUnit tu) {
    return new LogAttribute("execUs", tu.toMicros(time));
  }

  public static LogAttribute<Long> value(final String what, final long value) {
    return new LogAttribute(what, value);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(32);
    writeJsonTo(result);
    return result.toString();
  }

  @Override
  public void writeJsonTo(final Appendable appendable) throws IOException {
    JsonGenerator gen = Json.FACTORY.createGenerator(new AppendableWriter(appendable));
    gen.setCodec(Json.MAPPER);
    writeJsonTo(gen);
    gen.flush();
  }

  public void writeJsonTo(final JsonGenerator gen) throws IOException {
    gen.writeStartObject();
    gen.writeFieldName(first);
    gen.writeObject(second);
    gen.writeEndObject();
  }

  public static LogAttribute<Object> fromJson(final CharSequence jsonStr) {
    try {
      JsonParser parser = Json.FACTORY.createParser(CharSequences.reader(jsonStr));
      parser.setCodec(Json.MAPPER);
      Map<String, Object> val = parser.readValueAs(Map.class);
      return fromMap(val);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static LogAttribute<Object> fromMap(final Map<String, Object> val) {
    if (val.size() != 1) {
      throw new IllegalArgumentException("No Log Attribute: " + val);
    }
    Map.Entry<String, Object> entry = val.entrySet().iterator().next();
    Object value = entry.getValue();
    return LogAttribute.of(entry.getKey(), value);
  }

  @Override
  public void add(final Marker reference) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(final Marker reference) {
    return false;
  }

  @Override
  public boolean hasChildren() {
    return false;
  }

  @Override
  public boolean hasReferences() {
    return false;
  }

  @Override
  public Iterator<Marker> iterator() {
    return Collections.emptyListIterator();
  }

  @Override
  public boolean contains(final Marker other) {
    return false;
  }

  @Override
  public boolean contains(final String name) {
    return false;
  }

  private Object writeReplace()
          throws java.io.ObjectStreamException {
    StringBuilder sb = new StringBuilder(32);
    writeJsonTo(sb);
    return new AttrProxy(sb);
  }

  private static final class AttrProxy implements Serializable {

    private static final long serialVersionUID = 1L;
    private StringBuilder json;

    AttrProxy(final StringBuilder json) {
      this.json = json;
    }

    private Object readResolve() {
      return LogAttribute.fromJson(json);
    }

  }

}
