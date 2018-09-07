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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class PrintLogConfigsIO {

  private PrintLogConfigsIO() {
  }

  public static Map<String, PrintConfig> loadConfig(final Path path) throws IOException {
    try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return loadConfig(br);
    }
  }

  @Nullable
  public static Map<String, PrintConfig> loadConfigFromResource(final String resourceName) {
    ClassLoader loader = MoreObjects.firstNonNull(
            Thread.currentThread().getContextClassLoader(), Resources.class.getClassLoader());
    URL url = loader.getResource(resourceName);
    if (url == null) {
      return null;
    }
    try (BufferedReader cfgR = Resources.asCharSource(url, StandardCharsets.UTF_8).openBufferedStream()) {
      return loadConfig(cfgR);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static Map<String, PrintConfig> loadConfig(final Reader reader) throws IOException {
    Properties props = new Properties();
    props.load(reader);
    Map<String, PrintConfig> cfgs = Maps.newHashMapWithExpectedSize(props.size());
    Set<Map.Entry<String, String>> entrySet = (Set) props.entrySet();
    for (Map.Entry<String, String> entry : entrySet) {
      String category = entry.getKey().trim();
      String[] cfg = entry.getValue().trim().split(",");
      int len = cfg.length;
      if (len == 0) {
        throw new IllegalArgumentException("Invalid config entry: " + entry);
      }
      final boolean greedy;
      String lval = cfg[len - 1];
      if ("true".equalsIgnoreCase(lval)) {
        greedy = true;
        len--;
      } else if ("false".equalsIgnoreCase(lval)) {
        greedy = false;
        len--;
      } else {
        greedy = false;
      }
      if (len != 1) {
        throw new IllegalArgumentException("Invalid config entry: " + entry);
      }
      final Level minLevel = Level.valueOf(cfg[0]);
      cfgs.put(category, new PrintConfig(category, minLevel, greedy));
    }
    return cfgs;
  }

}
