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
package org.spf4j.base;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 *
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class Env {

  private Env() { }

  public static int getValue(final String envname, final int defaultValue) {
    String v = System.getenv(envname);
    if (v == null) {
      return defaultValue;
    } else {
      return Integer.parseInt(v);
    }
  }

  public static float getValue(final String envname, final float defaultValue) {
    String v = System.getenv(envname);
    if (v == null) {
      return defaultValue;
    } else {
      return Float.parseFloat(v);
    }
  }

  public static double getValue(final String envname, final double defaultValue) {
    String v = System.getenv(envname);
    if (v == null) {
      return defaultValue;
    } else {
      return Float.parseFloat(v);
    }
  }

  public static boolean getValue(final String envname, final boolean defaultValue) {
    String v = System.getenv(envname);
    if (v == null) {
      return defaultValue;
    } else {
      return Boolean.parseBoolean(v);
    }
  }

  @NonNull
  public static String getValue(final String envname, final String defaultValue) {
    String v = System.getenv(envname);
    if (v == null) {
      return defaultValue;
    } else {
      return v;
    }
  }

  @NonNull
  public static String getValue(final String envname, final Supplier<String> defaultValue) {
    String v = System.getenv(envname);
    if (v == null) {
      return defaultValue.get();
    } else {
      return v;
    }
  }

  @NonNull
  public static String getSystemProperty(final String name, final String[] deprecatedAliases,
          final String defaultValue) {
    String val = getSystemProperty(name, deprecatedAliases);
    if (val != null) {
      return val;
    }
    return defaultValue;
  }

  @Nullable
  public static String getSystemProperty(final String name, final String... deprecatedAliases) {
    String val = System.getProperty(name);
    if (val != null) {
      return val;
    }
    for (String key : deprecatedAliases) {
      val = System.getProperty(key);
      if (val != null) {
        Logger.getLogger(Env.class.getName()).log(Level.WARNING,
                "Use {0} system property instead of {1}", new Object[] {name, key});
        return val;
      }
    }
    return null;
  }
}
