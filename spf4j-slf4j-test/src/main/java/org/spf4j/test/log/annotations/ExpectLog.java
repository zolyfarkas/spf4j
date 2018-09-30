/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.test.log.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import org.spf4j.test.log.Level;

/**
 * Annotation to assert log behavior.
 * @author Zoltan Farkas
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ExpectLogs.class)
public @interface ExpectLog {
  /**
   * @return the log category to print. ("" is the root category).
   */
  String category() default "";
  /**
   * @return expected log level.
   */
  Level level() default Level.ERROR;
  /**
   * @return regexp of expected log message.
   */
  String messageRegexp() default ".*";

  /**
   * @return nr of times the expectation to be asserted.
   */
  int nrTimes() default 1;

  /**
   * @return the number of TU to wait for a log message after the unit test execution is finished.
   */
  long expectationTimeout() default 0;

  /**
   * @return expectation timeout unit.
   */
  TimeUnit timeUnit()  default TimeUnit.MILLISECONDS;

}