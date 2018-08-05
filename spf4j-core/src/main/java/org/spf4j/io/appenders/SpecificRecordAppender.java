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
package org.spf4j.io.appenders;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.activation.MimeType;
import org.apache.avro.Schema;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.spf4j.base.EscapeJsonStringAppendableWrapper;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.JThrowable;
import org.spf4j.io.AppendableOutputStream;
import org.spf4j.io.MimeTypes;
import org.spf4j.io.ObjectAppender;

/**
 *
 * @author zoly
 */
public final class SpecificRecordAppender implements ObjectAppender<SpecificRecord> {

  /**
   * Strict serialization means if something goes wrong with serializing an avro object.
   * A exception will be thrown.
   * If lenient serialization is being used a JSON representation of the error along with a toString representation
   * of the object will be serialized in case something goes wrong with the serialization.
   */
  private static final boolean STRICT_SERIALIZATION = Boolean.getBoolean("spf4j.strictAvroObjectAppenders");

  static final EncoderFactory EF = new EncoderFactory();

  static final ThreadLocal<StringBuilder> TMP = new ThreadLocal<StringBuilder>() {
    @Override
    protected StringBuilder initialValue() {
      return new StringBuilder(64);
    }

  };

  @Override
  public MimeType getAppendedType() {
    return MimeTypes.APPLICATION_JSON;
  }

  @Override
  public void append(final SpecificRecord object, final Appendable appendTo) throws IOException {
    StringBuilder sb = TMP.get();
    sb.setLength(0);
    try (AppendableOutputStream bos = new AppendableOutputStream(sb, StandardCharsets.UTF_8)) {
      final Schema schema = object.getSchema();
      SpecificDatumWriter<SpecificRecord> writer = new SpecificDatumWriter<>(schema);
      JsonEncoder jsonEncoder = EF.jsonEncoder(schema, bos);
      writer.write(object, jsonEncoder);
      jsonEncoder.flush();
    } catch (IOException | RuntimeException ex) {
      writeSerializationError(object, sb, ex);
    }
    appendTo.append(sb);
  }

  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  static void writeSerializationError(final Object object, final StringBuilder sb, final Exception ex)
          throws IOException {
    if (STRICT_SERIALIZATION) {
      if (ex instanceof IOException) {
        throw (IOException) ex;
      } else if (ex instanceof RuntimeException) {
        throw (RuntimeException) ex;
      } else {
        throw new IllegalStateException(ex);
      }
    }
    sb.setLength(0);
    sb.append("{\"SerializationError\":\n");
    try (AppendableOutputStream bos = new AppendableOutputStream(sb, StandardCharsets.UTF_8)) {
      JThrowable at = Converters.convert(ex);
      Schema schema = at.getSchema();
      SpecificDatumWriter<SpecificRecord> writer = new SpecificDatumWriter<>(schema);
      JsonEncoder jsonEncoder = EF.jsonEncoder(schema, bos, true);
      writer.write(at, jsonEncoder);
      jsonEncoder.flush();
    }
    sb.append(",\n");
    sb.append("\"ObjectAsString\":\n\"");
    EscapeJsonStringAppendableWrapper escaper = new EscapeJsonStringAppendableWrapper(sb);
    escaper.append(object.toString());
    sb.append("\"}");
  }

}
