/*
 * Copyright 2020 SPF4J.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class ExtendedNames extends Schema.Names {

  public ExtendedNames() {
  }

  public ExtendedNames(final String space) {
    super(space);
  }

  /**
   * overwrite for extra name resolution.
   * @param o
   * @return
   */
  @Override
  public Schema get(final String o) {
    return super.get(o);
  }


  /**
   * overwrite for extra name resolution.
   * @param o
   * @return
   */
  @Override
  public Schema get(final Object o) {
    return super.get(o);
  }

}
