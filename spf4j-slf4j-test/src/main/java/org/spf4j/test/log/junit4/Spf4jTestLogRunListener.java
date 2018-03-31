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
package org.spf4j.test.log.junit4;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * @author Zoltan Farkas
 */
public class Spf4jTestLogRunListener extends RunListener {

  private final Spf4jTestLogRunListenerSingleton instance;

  public Spf4jTestLogRunListener() {
    instance = Spf4jTestLogRunListenerSingleton.getInstance();
  }

  @Override
  public final void testIgnored(final Description description) throws Exception {
    instance.testIgnored(description);
  }

  @Override
  public final void testAssumptionFailure(final Failure failure) {
    instance.testAssumptionFailure(failure);
  }

  @Override
  public final void testFailure(final Failure failure) {
    instance.testFailure(failure);
  }

  @Override
  public final void testFinished(final Description description) {
    instance.testFinished(description);
  }

  @Override
  public final void testStarted(final Description description) throws Exception {
    instance.testStarted(description);
  }

  @Override
  public final void testRunFinished(final Result result) {
    instance.testRunFinished(result);
  }

  @Override
  public final void testRunStarted(final Description description) throws Exception {
    instance.testRunStarted(description);
  }

  @Override
  public final String toString() {
    return "Spf4jTestLogRunListener{" + "instance=" + instance + '}';
  }

}
