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
package org.spf4j.junit;

import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * A utility JUnit RunListenerRegister that allows registering run listeners dynamically with a static method.
 * @author zoly
 */
public final class RunListenerRegister extends RunListener {

  private static RunListenerRegister listenerRegister;

  private final CopyOnWriteArrayList<RunListener> listeners;

  public RunListenerRegister() {
    listeners = new CopyOnWriteArrayList<>();
    synchronized (RunListenerRegister.class) {
      if (listenerRegister == null) {
        listenerRegister = this;
      } else {
        throw new ExceptionInInitializerError("Attempting to instantiate second RunListenerRegister instance: "
                + this + ", existing: " + listenerRegister);
      }
    }
  }

  public static boolean tryAddRunListener(final RunListener listener, final boolean first) {
    synchronized (RunListenerRegister.class) {
      if (listenerRegister != null) {
        if (first) {
          listenerRegister.listeners.add(0, listener);
        } else {
          listenerRegister.listeners.add(listener);
        }
        return true;
      } else {
        return false;
      }
    }
  }

  public static void addRunListener(final RunListener listener, final boolean first) {
    if (!tryAddRunListener(listener, first)) {
      throw new IllegalStateException("Cannot register " + listener
                + " please start junit with org.spf4j.junit.RunListenerRegister run listener,"
                + "for more detail see http://maven.apache.org/surefire/maven-surefire-plugin/examples/junit.html");
    }
  }


  public static boolean removeRunListener(final RunListener listener) {
    synchronized (RunListenerRegister.class) {
      if (listenerRegister != null) {
        return listenerRegister.listeners.remove(listener);
      } else {
        return false;
      }
    }
  }

  @Override
  public void testIgnored(final Description description) throws Exception {
    for (RunListener listener : listeners) {
      listener.testIgnored(description);
    }
  }

  @Override
  public void testAssumptionFailure(final Failure failure) {
    for (RunListener listener : listeners) {
      listener.testAssumptionFailure(failure);
    }
  }

  @Override
  public void testFailure(final Failure failure) throws Exception {
    for (RunListener listener : listeners) {
      listener.testFailure(failure);
    }
  }

  @Override
  public void testFinished(final Description description) throws Exception {
    for (RunListener listener : listeners) {
      listener.testFinished(description);
    }
  }

  @Override
  public void testStarted(final Description description) throws Exception {
    for (RunListener listener : listeners) {
      listener.testStarted(description);
    }
  }

  @Override
  public void testRunFinished(final Result result) throws Exception {
    for (RunListener listener : listeners) {
      listener.testRunFinished(result);
    }
  }

  @Override
  public void testRunStarted(final Description description) throws Exception {
    for (RunListener listener : listeners) {
      listener.testRunStarted(description);
    }
  }

  @Override
  public String toString() {
    return "RunListenerRegister{" + "listeners=" + listeners + '}';
  }

}
