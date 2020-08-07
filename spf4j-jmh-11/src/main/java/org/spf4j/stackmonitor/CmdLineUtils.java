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
package org.spf4j.stackmonitor;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.util.Utils;

/**
 *
 * @author Zoltan Farkas
 */
public final class CmdLineUtils {

  private CmdLineUtils() {
  }

  public static OptionSet parseInitLine(final String initLine, final OptionParser parser) throws ProfilerException {
    parser.accepts("help", "Display help.");

    OptionSpec<String> nonOptions = parser.nonOptions();

    String[] split = initLine.split(";");
    for (int c = 0; c < split.length; c++) {
      if (!split[c].isEmpty()) {
        split[c] = "-" + split[c];
      }
    }

    OptionSet set;
    try {
      set = parser.parse(split);
    } catch (OptionException e) {
      try {
        StringWriter sw = new StringWriter();
        sw.append(e.getMessage());
        sw.append("\n");
        parser.printHelpOn(sw);
        ProfilerException pex = new ProfilerException(sw.toString());
        pex.addSuppressed(e);
        throw pex;
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    if (set.has("help")) {
      try {
        StringWriter sw = new StringWriter();
        parser.printHelpOn(sw);
        throw new ProfilerException(sw.toString());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    String s = set.valueOf(nonOptions);
    if (s != null && !s.isEmpty()) {
      throw new ProfilerException("Unhandled options: " + s + " in " + initLine);
    }
    return set;
  }

  public static final class ProfilerOptionFormatter implements HelpFormatter {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final String name;

    public ProfilerOptionFormatter(final String name) {
      this.name = name;
    }

    public String format(final Map<String, ? extends OptionDescriptor> options) {
      StringBuilder sb = new StringBuilder(128);
      sb.append("Usage: -prof <profiler-name>:opt1=value1,value2;opt2=value3");
      sb.append(LINE_SEPARATOR);
      sb.append(LINE_SEPARATOR);
      sb.append("Options accepted by ").append(name).append(':');
      for (OptionDescriptor each : options.values()) {
        try {
          lineFor(each, sb);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }

      return sb.toString();
    }

    private static void lineFor(final OptionDescriptor d, final Appendable line) throws IOException {

      StringBuilder o = new StringBuilder();
      o.append("  ");
      for (String str : d.options()) {
        if (d.representsNonOptions()) {
          continue;
        }
        o.append(str);
        if (d.acceptsArguments()) {
          o.append('=');
          if (d.requiresArgument()) {
            o.append('<');
          } else {
            o.append('[');
          }
          o.append(d.argumentDescription());
          if (d.requiresArgument()) {
            o.append('>');
          } else {
            o.append(']');
          }
        }
      }

      final int optWidth = 35;

      line.append(String.format("%-" + optWidth + 's', o.toString()));
      boolean first = true;
      String desc = d.description();
      List<?> defaults = d.defaultValues();
      if (defaults != null && !defaults.isEmpty()) {
        desc += " (default: " + defaults + ')';
      }
      for (String l : Utils.rewrap(desc)) {
        if (first) {
          first = false;
        } else {
          line.append(LINE_SEPARATOR);
          line.append(String.format("%-" + optWidth + 's', ""));
        }
        line.append(l);
      }

      line.append(LINE_SEPARATOR);
      line.append(LINE_SEPARATOR);
    }

  }

}
