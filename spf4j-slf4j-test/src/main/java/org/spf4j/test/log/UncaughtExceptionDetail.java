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

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * @author Zoltan Farkas
 */
public final class UncaughtExceptionDetail {

  private final Thread thread;
  private final Throwable throwable;

  public UncaughtExceptionDetail(final Thread thread, final Throwable throwable) {
    this.thread = thread;
    this.throwable = throwable;
  }

  public Thread getThread() {
    return thread;
  }

  public Throwable getThrowable() {
    return throwable;
  }

  @Override
  public String toString() {
    return "UncaughtException{" + "thread=" + thread + ", throwable=" + throwable + '}';
  }

  public static Matcher<UncaughtExceptionDetail> hasThrowable(final Matcher<Throwable> tMatcher) {
    return Matchers.hasProperty("throwable", tMatcher);
  }

  public static Matcher<UncaughtExceptionDetail> hasThread(final Matcher<Thread> tMatcher) {
    return Matchers.hasProperty("thread", tMatcher);
  }

}
