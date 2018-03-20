/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
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
package org.spf4j.zel.vm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.spf4j.base.CharSequences;
import org.spf4j.zel.instr.Instruction;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class ZelFrame implements Serializable {

  private static final ConcurrentMap<String, CharSequence> SOURCES = new ConcurrentHashMap();

  private static final AtomicInteger IDX = new AtomicInteger();
  private static final long serialVersionUID = 1L;

  private final String programName;

  private final String source;

  private final int index;

  public static String newSource(final String sourceDetail) {
    String id = "zel_" + IDX.getAndIncrement();
    SOURCES.put(id, CharSequences.toLineNumbered(0, sourceDetail));
    return id;
  }

  public static void annotate(final String id, final Program program) {
    CharSequence source = SOURCES.get(id);
    String[] lines = source.toString().split("\n");
    StringBuilder[] annotations = new StringBuilder[lines.length];
    Instruction[] instructions = program.getInstructions();
    ParsingContext.Location[] debug = program.getDebug();
    for (int i = 0; i < instructions.length; i++) {
      ParsingContext.Location loc = debug[i];
      int lineNr = loc.getRow();
      if (lineNr < 0) {
        continue;
      }
      StringBuilder existing = annotations[lineNr];
      if (existing == null) {
        existing = new StringBuilder();
        existing.append(" /* ").append(instructions[i]);
        annotations[lineNr] = existing;
      } else {
        existing.append(';').append(instructions[i]);
      }
    }
    StringBuilder result = new StringBuilder(source.length() * 2);
    for (int i = 0; i < lines.length; i++) {
      result.append(lines[i]);
      StringBuilder annotation = annotations[i];
      if (annotation != null) {
        result.append(annotation).append(" */\n");
      } else {
        result.append('\n');
      }
    }
    SOURCES.put(id, result);
  }

  public static CharSequence getDetail(final String id) {
    return SOURCES.get(id);
  }

  public ZelFrame(final String programName, final String source, final int index) {
    this.programName = programName;
    this.index = index;
    this.source = source;
  }

  @Override
  public String toString() {
    return programName + '(' + source + ':' + index + ')';
  }

  public String getProgramName() {
    return programName;
  }

  public String getSource() {
    return source;
  }

  public int getIndex() {
    return index;
  }

}
