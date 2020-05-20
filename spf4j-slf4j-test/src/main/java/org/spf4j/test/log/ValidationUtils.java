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
package org.spf4j.test.log;

import org.slf4j.Logger;

/**
 * Utilities to validate correct Test logger framework setup.
 *
 * @author Zoltan Farkas
 */
public final class ValidationUtils {

  private ValidationUtils() { }

  public static final void validateLogger(final Logger log) {
    if (!(log instanceof TestLogger)) {
      throw new ExceptionInInitializerError("Incorrect logging backend is picked up, please make sure:\n"
              + "     <dependency>\n"
              + "      <groupId>org.spf4j</groupId>\n"
              + "      <artifactId>spf4j-slf4j-test</artifactId>\n"
              + "      <scope>test</scope>\n"
              + "      <version>${project.version}</version>\n"
              + "    </dependency>\n is before any other slf4j logging backed (logback, etc...) in your dependencies");
    }
  }

}
