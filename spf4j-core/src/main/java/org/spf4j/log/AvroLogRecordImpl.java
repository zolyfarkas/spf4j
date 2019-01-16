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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Marker;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.LogRecord;

/**
 *
 * @author Zoltan Farkas
 */
public final class AvroLogRecordImpl implements Slf4jLogRecord {

  private final LogRecord record;

  private boolean isLogged;

  public AvroLogRecordImpl(final LogRecord record) {
    this.record = record;
    this.isLogged = false;
  }

  @Override
  public LogRecord toLogRecord(final String origin, final String traceId) {
    return record;
  }

  @Override
  public String getOrigin() {
    return record.getOrigin();
  }

  @Override
  public Object[] getArguments() {
    List xtra = record.getXtra();
    return xtra.toArray(new Object[xtra.size()]);
  }

  @Override
  public Object[] getExtraArgumentsRaw() {
    return getArguments();
  }

  @Override
  public Object[] getExtraArguments() {
    return getArguments();
  }

  @Override
  public Throwable getExtraThrowable() {
    return Converters.convert(record.getOrigin(), record.getThrowable());
  }

  @Override
  public Level getLevel() {
    return Level.fromAvroLevel(record.getLevel());
  }

  @Override
  public String getLoggerName() {
    return record.getLogger();
  }

  @Override
  @Nullable
  public Marker getMarker() {
    return null;
  }

  @Override
  public String getMessage() {
    return record.getMsg();
  }

  @Override
  public String getMessageFormat() {
    return getMessage();
  }

  @Override
  public int getNrMessageArguments() {
    return 0;
  }

  @Override
  public String getThreadName() {
    return record.getThr();
  }

  @Override
  public long getTimeStamp() {
    return record.getTs().toEpochMilli();
  }

  @Override
  public synchronized boolean isLogged() {
    return isLogged;
  }

  @Override
  public synchronized void setIsLogged() {
    this.isLogged = true;
  }

  @Override
  public void attach(final Object obj) {
    // do not attach;
  }

  @Override
  public Set<Object> getAttachments() {
    return Collections.EMPTY_SET;
  }

  @Override
  public boolean hasAttachment(final Object obj) {
    return false;
  }

  @Override
  public String toString() {
    return "AvroLogRecordImpl{" + "record=" + record + ", isLogged=" + isLogged + '}';
  }

}
