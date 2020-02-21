/*
 * Copyright 2019 SPF4J.
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
package org.spf4j.ui;

import java.io.File;

/**
 * @author Zoltan Farkas
 */
public final class Spf4jFileFilter extends ComposableFileFilter {

  public static final Spf4jFileFilter TSDB = new Spf4jFileFilter("tsdb");
  public static final Spf4jFileFilter TSDB2 = new Spf4jFileFilter("tsdb2");
  public static final Spf4jFileFilter SSDUMP = new Spf4jFileFilter("ssdump");
  public static final Spf4jFileFilter SSDUMP2 = new Spf4jFileFilter("ssdump2");
  public static final Spf4jFileFilter SSDUMP3 = new Spf4jFileFilter("ssdump3");
  public static final Spf4jFileFilter SSDUMP2_GZ = new Spf4jFileFilter("ssdump2.gz");
  public static final Spf4jFileFilter SSDUMP3_GZ = new Spf4jFileFilter("ssdump3.gz");
  public static final Spf4jFileFilter D3_JSON = new Spf4jFileFilter("d3.json");
  public static final Spf4jFileFilter SPF4J_JSON = new Spf4jFileFilter("spf4j.json");
  public static final Spf4jFileFilter AVRO_TABLEDEF = new Spf4jFileFilter("tabledef.avro");

  private final String type;

  private Spf4jFileFilter(final String type) {
    this.type = "." + type;
  }

  @Override
  public boolean accept(final File f) {
    return f.getName().endsWith(type);
  }

  @Override
  public String getDescription() {
    return "*" + type;
  }

  public String getSuffix() {
    return type;
  }


  @Override
  public String toString() {
    return "Spf4jFileFilter{" + "type=" + type + '}';
  }

}
