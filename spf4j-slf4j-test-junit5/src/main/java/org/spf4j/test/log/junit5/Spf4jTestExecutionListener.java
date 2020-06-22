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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.runner.Description;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Arrays;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.test.log.junit4.Spf4jTestLogRunListenerSingleton;
import org.spf4j.test.log.junit4.TestBaggage;

/**
 *
 * @author Zoltan Farkas
 */
public final class Spf4jTestExecutionListener implements TestExecutionListener {

  static {
    System.setProperty("junit.jupiter.extensions.autodetection.enabled", "true");
    System.setProperty("org.jboss.logging.provider", "slf4j");
    LoggerFactory.getLogger(Spf4jTestExecutionListener.class).info("Spf4jTestExecutionListener initialized");
  }

  private final int maxDebugLogsCollected = Integer.getInteger("spf4j.test.log.collectmaxLogs", 100);

  private static Annotation[] getMethodAnnotations(final String className,
          final String methodName, final String paramsCsv) {
    try {
      Class<?>[] pTypes;
      if (paramsCsv.isEmpty()) {
        pTypes = Arrays.EMPTY_CLASS_ARRAY;
      } else {
        List<String> params = Csv.readRow(paramsCsv);
        pTypes = new Class[params.size()];
        int i = 0;
        for (String pType : params) {
          pTypes[i++] = Class.forName(pType);
        }
      }
      return Class.forName(className).getMethod(methodName, pTypes).getAnnotations();
    } catch (CsvParseException | ClassNotFoundException | NoSuchMethodException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void executionFinished(final TestIdentifier testIdentifier,
          final TestExecutionResult testExecutionResult) {
    Optional<TestSource> source = testIdentifier.getSource();
    if (source.isPresent()) {
      TestSource ts = source.get();
      if (ts instanceof MethodSource) {
        MethodSource ms = (MethodSource) ts;
        try {
          String className = ms.getClassName();
          String methodName = ms.getMethodName();
          Description description = Description.createTestDescription(className, methodName,
                  getMethodAnnotations(className, methodName, ms.getMethodParameterTypes()));
          ExecutionContext ctx = ExecutionContexts.current();
          if (ctx != null) {
            TestBaggage tb = ctx.get(Spf4jTestLogRunListenerSingleton.BAG_TAG);
            if (testExecutionResult.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
              Spf4jTestLogRunListenerSingleton.dumpDebugInfoOnFailure(tb, description, maxDebugLogsCollected);
            }
            Spf4jTestLogRunListenerSingleton.cleanupAfterTestFinish(description, tb);
          }
        } catch (RuntimeException ex) {
          throw ex;
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    }
  }



  @Override
  public String toString() {
    return "Spf4jTestExecutionListener";
  }

}
