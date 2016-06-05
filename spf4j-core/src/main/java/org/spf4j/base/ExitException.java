
package org.spf4j.base;

/**
 * @author zoly
 */
public final class ExitException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int exitCode;

  public ExitException(final int exitCode) {
    super("Exited with " + exitCode);
    this.exitCode = exitCode;
  }

  public int getExitCode() {
    return exitCode;
  }

}
