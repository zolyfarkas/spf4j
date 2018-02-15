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
package org.spf4j.test.log;

import java.util.List;

/**
 * @author Zoltan Farkas
 */
public final class TestUtils {
  private TestUtils() { }

  public static boolean isExecutedFromNetbeans() {
    String mvnNetbeansCP = System.getProperty("maven.ext.class.path");
    return mvnNetbeansCP != null && mvnNetbeansCP.contains("netbeans");
  }

  /**
   * Supporting netbeans only at this time.
   * For other IDEs you need to configure them to pass the spf4j.execFromIDE property.
   * @return
   */
  public static boolean isExecutedFromIDE() {
    return isExecutedFromNetbeans() || System.getProperty("spf4j.execFromIDE") != null;
  }

  public static boolean isExecutedWithDebuggerAgent() {
    List<String> inputArguments = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
    return inputArguments.stream()
            .filter((arg) -> "-agentlib:jdwp".equals(arg) || "-Xdebug".equals(arg) || arg.startsWith("-Xrunjdwp"))
            .count() > 0;

  }

}
