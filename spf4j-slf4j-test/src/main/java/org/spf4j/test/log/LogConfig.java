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
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public interface LogConfig {

  @Nullable
  LogConsumer getLogConsumer(String category, Level level);

  LogConfig add(String category, LogHandler handler, ToIntFunction<List<LogHandler>> whereTo);

  default LogConfig add(final String category, final LogHandler handler) {
    return add(category, handler, (l) -> 0);
  }

  LogConfig remove(String category, LogHandler handler);

  default Level minRootLevel() {
    for (Level l : Level.values()) {
      if (getLogConsumer("", l) != null) {
        return l;
      }
    }
    return Level.ERROR;
  }


}
