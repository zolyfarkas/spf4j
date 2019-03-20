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
package org.spf4j.maven.plugin.avro.avscp;

/**
 *
 * @author Zoltan Farkas
 */
public final class SourceLocation {

  private final String filePath;

  private final int lineNr;

  private final int colNr;

  public SourceLocation(final String encoded) {
    int cidxS = encoded.lastIndexOf(':');
    colNr = Integer.parseInt(encoded.substring(cidxS + 1));
    int ridxS = encoded.lastIndexOf(':', cidxS - 1);
    lineNr = Integer.parseInt(encoded.substring(ridxS + 1, cidxS));
    filePath = encoded.substring(0, ridxS);
  }


  public SourceLocation(final String filePath, final int lineNr, final int colNr) {
    this.filePath = filePath;
    this.lineNr = lineNr;
    this.colNr = colNr;
  }

  public String getFilePath() {
    return filePath;
  }

  public int getLineNr() {
    return lineNr;
  }

  public int getColNr() {
    return colNr;
  }

  @Override
  public String toString() {
    return filePath + ':' + lineNr + ':' + colNr;
  }

}
