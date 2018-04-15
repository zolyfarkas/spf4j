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

import org.spf4j.test.log.ExceptionHandoverRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.test.log.UncaughtExceptionDetail;
import org.spf4j.test.log.UncaughtExceptionConsumer;

/**
 * @author Zoltan Farkas
 */
class ExceptionAsserterUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler, ExceptionHandoverRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(ExceptionAsserterUncaughtExceptionHandler.class);

  private final Thread.UncaughtExceptionHandler wrapped;

  private final List<UncaughtExceptionConsumer> handovers;

  private final List<UncaughtExceptionDetail> uncaughtExceptions;

  ExceptionAsserterUncaughtExceptionHandler(final Thread.UncaughtExceptionHandler wrapped) {
    this.wrapped = wrapped;
    this.uncaughtExceptions = new CopyOnWriteArrayList<>();
    this.handovers = new CopyOnWriteArrayList<>();
  }

  @Override
  public void add(final UncaughtExceptionConsumer handover) {
    handovers.add(handover);
  }

  @Override
  public void remove(final UncaughtExceptionConsumer handover) {
    handovers.remove(handover);
  }

  @Override
  public void uncaughtException(final Thread t, final Throwable e) {
    if (wrapped != null) {
      wrapped.uncaughtException(t, e);
    } else {
      LOG.debug("Uncaught Exception in thread {}", t, e);
    }
    UncaughtExceptionDetail exDetail = new UncaughtExceptionDetail(t, e);
    boolean accepted = false;
    for (UncaughtExceptionConsumer handover : handovers) {
      if (handover.offer(exDetail)) {
        accepted = true;
      }
    }
    if (!accepted) {
      uncaughtExceptions.add(exDetail);
    }
  }

  public List<UncaughtExceptionDetail> getUncaughtExceptions() {
    return new ArrayList<>(uncaughtExceptions);
  }

  @Override
  public String toString() {
    return "UncaughtExceptionAsserter{" + "wrapped=" + wrapped + ", handovers="
            + handovers + ", uncaughtExceptions=" + uncaughtExceptions + '}';
  }



}
