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

import java.util.Objects;

/**
 * @author Zoltan Farkas
 */
public final class PrintConfig {

  /**
   * the log category to print. ("" is the root category).
   */
  private final String category;
  /**
   *  true if we don't want downstream log handlers to receive any logs from this category.
   */
  private final boolean greedy;
  /**
   *  minimum log level to print.
   */
  private final Level minLevel;

  public PrintConfig(final String category, final Level minLevel, final boolean greedy) {
    this.category = category;
    this.greedy = greedy;
    this.minLevel = minLevel;
  }

  public String getCategory() {
    return category;
  }

  public boolean isGreedy() {
    return greedy;
  }

  public Level getMinLevel() {
    return minLevel;
  }

  @Override
  public int hashCode() {
    int hash = 11 + Objects.hashCode(this.category);
    hash = 11 * hash + (this.greedy ? 1 : 0);
    return 11 * hash + Objects.hashCode(this.minLevel);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PrintConfig other = (PrintConfig) obj;
    if (this.greedy != other.greedy) {
      return false;
    }
    if (!Objects.equals(this.category, other.category)) {
      return false;
    }
    return this.minLevel == other.minLevel;
  }



  @Override
  public String toString() {
    return "PrintConfig{" + "category=" + category + ", greedy=" + greedy + ", minLevel=" + minLevel + '}';
  }



}
