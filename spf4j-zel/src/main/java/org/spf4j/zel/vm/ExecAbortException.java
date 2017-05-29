package org.spf4j.zel.vm;

/**
 *
 * @author zoly
 */
public final class ExecAbortException extends RuntimeException {

  public static final ExecAbortException INSTANCE = new ExecAbortException();

  @Override
  public Throwable fillInStackTrace() {
    return this;
  }
}
