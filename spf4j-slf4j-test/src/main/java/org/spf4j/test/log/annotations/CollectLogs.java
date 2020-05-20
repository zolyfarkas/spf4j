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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.spf4j.log.Level;

/**
 * Annotation to specify custom log collection for a particular unit test.
 * By default all unprinted logs above and including DEBUG level are collected for the purpose of being logged
 * in case of unit test failure.
 * @author Zoltan Farkas
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CollectLogs {

  /**
   * The minimum level of logs to collect.
   */
  Level minLevel() default Level.DEBUG;

  /**
   * Collect logs that have been printed(logged).
   */
  boolean collectPrinted() default false;

  /**
   * Maximum number of logs to collect.
   * @return
   */
  int nrLogs() default 256;

  /**
   * logs to include from collection. by default all categories are.
   * @return
   */
  String includeLogs() default "";

  /**
   * logs to exclude from collection.
   * @return
   */
  String[] excludeLogs() default {};
}