/*
 * Copyright 2019 SPF4J.
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
package org.apache.avro;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.avro.Schema.Names;

/**
 *
 * @Override public String getId(Schema schema) { return sResolver.getId(schema); }
 * @author Zoltan Farkas
 */
public final class SchemaRefWriter {

  private static final boolean SCHEMA_REFS_SUPORTED;

  static {
    boolean schemaRefsSupported;
    try {
      Names.class.getMethod("getId", Schema.class);
      schemaRefsSupported = true;
    } catch (NoSuchMethodException | SecurityException ex) {
      schemaRefsSupported = false;
    }
    SCHEMA_REFS_SUPORTED = schemaRefsSupported;

  }

  private SchemaRefWriter() { }

  public static boolean isSchemaRefsSupported() {
    return SCHEMA_REFS_SUPORTED;
  }

  private static final class NamesExt extends Names {

    private static final long serialVersionUID = 1L;

    private final String exclude;

    NamesExt(final String exclude) {
      this.exclude = exclude;
    }

    @Nullable
    public String getId(final Schema schema) {
      String id = schema.getProp("mvnId");
      if (id == null) {
        return null;
      }
      if (Objects.equals(schema.getFullName(),  exclude)) {
        return null;
      }
      return id;
    }
  }

  public static void write(final Schema schema, final OutputStream os) throws IOException {
    JsonGenerator jgen = Schema.FACTORY.createGenerator(os);
    schema.toJson(SCHEMA_REFS_SUPORTED ? new NamesExt(schema.getFullName()) : new Names(), jgen);
    jgen.flush();
  }

}
