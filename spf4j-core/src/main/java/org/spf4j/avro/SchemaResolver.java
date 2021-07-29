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
package org.spf4j.avro;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.avro.Schema;

/**
 * @author Zoltan Farkas
 */
public interface SchemaResolver {

  /**
   * Lower level resolver implementation, that will write a schema in a "custom way"
   * @param schema
   * @param gen
   * @return
   * @throws IOException
   */
  default boolean customWrite(Schema schema, JsonGenerator gen) throws IOException {
    String ref = getId(schema);
    if (ref != null) {
        gen.writeStartObject();
        gen.writeFieldName(getJsonAttrName());
        gen.writeString(ref);
        gen.writeEndObject();
        return true;
    } else {
      return false;
    }
  }

  /**
   * Lowere level resolver implementation that will read a schema from a "custom way"
   * @param object
   * @return the resolved schema, or null if nothing to resolve.
   */
  @Nullable
  default Schema customRead(Function<String, JsonNode> object) {
    JsonNode refVal = object.apply(getJsonAttrName());
    if (refVal != null) {
      return resolveSchema(refVal.asText());
    } else {
      return null;
    }
  }

  SchemaResolver NONE = new SchemaResolver() {
    @Override
    public boolean customWrite(final Schema schema, final JsonGenerator gen) {
      return false;
    }

    @Override
    public Schema customRead(final Function<String, JsonNode> object) {
      return null;
    }

    @Override
    public Schema resolveSchema(final String id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getId(final Schema schema) {
      return null;
    }
  };

  default String getJsonAttrName() {
    return "$ref";
  }

  @Nonnull
  Schema resolveSchema(String id);

  @Nullable
  String getId(Schema schema);


}
