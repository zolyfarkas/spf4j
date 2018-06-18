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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
@ThreadSafe
public interface LogHandler  {

  enum Handling {
    /* Will handle this message, and potentially pass it downstream */
    HANDLE_PASS,
    /** Will handle this message, and will consume it. */
    HANDLE_CONSUME,
    /** Not handling these messages */
    NONE
  }

  /**
   * find out if this handler should be used for the given log level.
   * @param level the log level.
   * @return Handling enum that, specifies what this handler does with these messages.
   */
  Handling handles(Level level);

  /**
   * Handler handling method
   * @param record the log record.
   * @return return the log message potentially with an attachment,
   * or null if the handler does not want to pass the message downstream.
   */
  @Nullable
  LogRecord handle(LogRecord record);

}
