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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * @author Zoltan Farkas
 */
@Immutable
@ParametersAreNonnullByDefault
final class LogConfigImpl implements LogConfig {

  private static final Comparator<String> REV_STR_COMPARATOR = ((Comparator<String>) String::compareTo).reversed();

  private static final ThreadLocal<ArrayList<LogHandler>> HNDLRS
          = new ThreadLocal<ArrayList<LogHandler>>() {
    @Override
    protected ArrayList<LogHandler> initialValue() {
      return new ArrayList<>(2);
    }
  };

  private final ImmutableList<LogHandler> rootHandler;

  private final SortedMap<String, List<LogHandler>> logHandlers;

  LogConfigImpl(final ImmutableList<LogHandler> rootHandler, final Map<String, List<LogHandler>> catHandlers) {
    this.rootHandler = rootHandler;
    logHandlers = new TreeMap<>(REV_STR_COMPARATOR);
    logHandlers.putAll(catHandlers);
  }

  LogConfigImpl add(final String category, final LogHandler handler) {
    ImmutableList<LogHandler> rh;
    Map<String, List<LogHandler>> ch;
    if (category.isEmpty()) {
      rh = ImmutableList.<LogHandler>builder().addAll(rootHandler).add(handler).build();
      ch = logHandlers;
    } else {
      rh = rootHandler;
      ch = new HashMap<>(logHandlers);
      List<LogHandler> hndlrs = ch.get(category);
      if (hndlrs == null) {
        hndlrs = new ArrayList<>(2);
        ch.put(category, hndlrs);
      }
      hndlrs.add(handler);
    }
    return new LogConfigImpl(rh, ch);
  }


  LogConfigImpl remove(final String category, final LogHandler handler) {
    ImmutableList<LogHandler> rh;
    Map<String, List<LogHandler>> ch;
    if (category.isEmpty()) {
      rh = rootHandler.stream().filter((h) ->  h != handler).collect(ImmutableList.toImmutableList());
      ch = logHandlers;
    } else {
      rh = rootHandler;
      ch = new HashMap<>(logHandlers);
      List<LogHandler> hndlrs = ch.get(category);
      if (hndlrs != null) {
        hndlrs.remove(handler);
        if (hndlrs.isEmpty()) {
          ch.remove(category);
        }
      }
    }
    return new LogConfigImpl(rh, ch);
  }

  @Override
  public List<LogHandler> getLogHandlers(final String category, final Level level) {
    if (category.isEmpty() || logHandlers.isEmpty()) {
      return rootHandler;
    }
    ArrayList<LogHandler> res = HNDLRS.get();
    res.clear();
    for (Map.Entry<String, List<LogHandler>> entry : logHandlers.tailMap(category).entrySet()) {
      String key = entry.getKey();
      if (category.startsWith(key)) {
        addAll(level, entry.getValue(), res);
      } else if (key.charAt(0) != category.charAt(0)) {
        break;
      }
    }
    addAll(level, rootHandler, res);
    return res;
  }

  private static void addAll(final Level level, final List<LogHandler> from, final List<LogHandler> to) {
    for (LogHandler handler : from) {
      if (handler.handles(level)) {
        to.add(handler);
      }
    }
  }

  Level minRootLevel() {
    for (Level l : Level.values()) {
      List<LogHandler> rlh = getLogHandlers("", l);
      if (!rlh.isEmpty()) {
        return l;
      }
    }
    return Level.ERROR;
  }

  @Override
  public String toString() {
    return "LogConfigImpl{" + "rootHandler=" + rootHandler + ", logHandlers=" + logHandlers + '}';
  }

}
