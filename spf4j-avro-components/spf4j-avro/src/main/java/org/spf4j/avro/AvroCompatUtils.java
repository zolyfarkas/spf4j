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
package org.spf4j.avro;

import java.lang.reflect.Constructor;
import org.apache.avro.Schema;
import org.spf4j.avro.official.OriginUtilInterface;
import org.spf4j.avro.zfork.ZFUtilInterface;
import org.spf4j.base.Reflections;

/**
 *
 * @author Zoltan Farkas
 */
public final class AvroCompatUtils {

  private static final UtilInterface INTF;

  static {
    Constructor<?> c = Reflections.getConstructor(Schema.Field.class, String.class, Schema.class,
            String.class,  Object.class,
            boolean.class, boolean.class, Schema.Field.Order.class);
    INTF = c == null ? new OriginUtilInterface() : new ZFUtilInterface();
  }

  private AvroCompatUtils() {
  }

  public interface UtilInterface {

    Schema.Field createField(String name, Schema schema, String doc,
            Object defaultVal,
            boolean validateDefault, boolean validateName, Schema.Field.Order order);
  }

  public static Schema.Field createField(final String name, final Schema schema, final String doc,
          final Object defaultVal,
          final boolean validateDefault, final boolean validateName, final Schema.Field.Order order) {
    return INTF.createField(name, schema, doc, defaultVal, validateDefault, validateName, order);
  }

}
