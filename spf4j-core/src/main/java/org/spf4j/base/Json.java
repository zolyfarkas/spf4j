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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.ServiceLoader;

/**
 * @author Zoltan Farkas
 */
public final class Json {

  private Json() { }

  public static final JsonFactory FACTORY = new JsonFactory();

  public static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);

  static {
    FACTORY.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    SimpleModule module = new SimpleModule("spf4j");
    loadServices(module);
    module.addSerializer(JsonWriteable.class, jsonWritableSerializer());
    MAPPER.registerModule(module);
    ServiceLoader<Module> loader = ServiceLoader.load(Module.class);
    for (Module mod : loader) {
      MAPPER.registerModule(mod);
    }
  }

  private static void loadServices(final SimpleModule module) {
    ServiceLoader<JsonSerializer> loader = ServiceLoader.load(JsonSerializer.class);
    for (JsonSerializer ser : loader) {
      module.addSerializer(ser);
    }
  }

  public static JsonSerializer<JsonWriteable> jsonWritableSerializer() {
    return new JsonSerializer<JsonWriteable>() {
      @Override
      public void serialize(final JsonWriteable value, final JsonGenerator jgen, final SerializerProvider provider)
              throws IOException {
        Object outputTarget = jgen.getOutputTarget();
        if (outputTarget instanceof Appendable) {
          jgen.flush();
          if (jgen.getOutputContext().getCurrentName() != null) {
            ((Appendable) outputTarget).append(':');
          }
          value.writeJsonTo((Appendable) outputTarget);
          return;
        }
        StringBuilder json = new StringBuilder(32);
        value.writeJsonTo(json);
        jgen.writeRawValue(json.toString());
      }
    };
  }

  public static JsonSerializer<Object> toStringJsonWritableSerializer() {
    return new JsonSerializer<Object>() {
      @Override
      public void serialize(final Object value, final JsonGenerator jgen, final SerializerProvider provider)
              throws IOException {
        jgen.writeString(value.toString());
      }
    };
  }


}
