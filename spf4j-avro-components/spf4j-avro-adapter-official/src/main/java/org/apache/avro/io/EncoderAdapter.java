/*
 * Copyright 2021 SPF4J.
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
package org.apache.avro.io;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import org.apache.avro.Schema;

/**
 * adapter to expose some package protected functionality.
 * @author Zoltan Farkas
 */
public final class EncoderAdapter {

  private EncoderAdapter() { }

  public static JsonEncoder jsonEncoder(final Schema schema, final JsonGenerator gen) throws IOException {
    return new JsonEncoder(schema, gen);
  }

}
