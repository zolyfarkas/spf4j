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
      System.err.println("Ignoring " + listener + " registration,"
                + " please start junit with RunListenerRegister run listener,"
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
