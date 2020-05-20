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
import org.spf4j.log.Level;

/**
 * Annotation to specify custom log printout for a particular unit test.
 *
 * The default log level is controlled via:
 *
 * <p>System properties:</p>
 * <ul>
 * <li>
 *   # default root log level when tests executed from IDE. see TestUtils.class for more info.
 *   spf4j.testLog.rootPrintLevelIDE = DEBUG
 * </li>
 * <li>
 *   # default root log level.
 *   spf4j.testLog.rootPrintLevel = INFO
 * </li>
 * </ul>
 * <p>Property files: spf4j-test-prtcfg.properties or spf4j-test-prtcfg-ide.properties for ide settings:</p>
 * <p>[category(package) name]=[LOG_LEVEL](,[greedy])?</p>
 *
 *
 * @author Zoltan Farkas
 * @see org.spf4j.test.log.annotations
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(PrintLogsConfigs.class)
public @interface PrintLogs {
  /**
   * @return the log category to print. ("" is the root category).
   */
  String category() default "";
  /**
   * @return true if we don't want downstream log handlers to receive any logs from this category.
   */
  boolean greedy() default false;
  /**
   * @return minimum log level to print.
   */
  Level minLevel() default Level.INFO;
  /**
   * @return minimum log level to print when running in the IDE.
   */
  Level ideMinLevel() default Level.DEBUG;
}