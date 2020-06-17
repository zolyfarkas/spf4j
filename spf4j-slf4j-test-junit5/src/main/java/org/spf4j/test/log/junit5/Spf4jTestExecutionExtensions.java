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
package org.spf4j.test.log.junit5;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.runner.Description;
import org.spf4j.test.log.junit4.Spf4jTestLogRunListenerSingleton;

/**
 *
 * @author Zoltan Farkas
 */
public final class Spf4jTestExecutionExtensions implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  private final Spf4jTestLogRunListenerSingleton instance;

  public Spf4jTestExecutionExtensions() {
    instance = Spf4jTestLogRunListenerSingleton
            .getOrCreateListenerInstance(Spf4jTestExecutionListener::getTimeoutMillis);
  }

  @Override
  public void beforeTestExecution(final ExtensionContext ec) {
    Method testMethod = ec.getRequiredTestMethod();
    try {
      String className = testMethod.getDeclaringClass().getName();
      String methodName = testMethod.getName();
      Description descr = Description.createTestDescription(className, methodName,
              testMethod.getAnnotations());
      instance.testStarted(descr);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void afterTestExecution(final ExtensionContext ec) {
    Method testMethod = ec.getRequiredTestMethod();
    try {
      String className = testMethod.getDeclaringClass().getName();
      String methodName = testMethod.getName();
      Description description = Description.createTestDescription(className, methodName,
              testMethod.getAnnotations());
      instance.assertionsAfterTestExecution(description);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public String toString() {
    return "Spf4jTestExecutionRegistrations{" + "instance=" + instance + '}';
  }





}
