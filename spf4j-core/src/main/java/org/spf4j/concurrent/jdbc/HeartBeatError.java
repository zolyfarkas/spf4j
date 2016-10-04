
package org.spf4j.concurrent.jdbc;

/**
 * A Error specifying that a Heartbeat cannot be done.
 * THis should be handled as a unrecoverable error in a distributed environment.
 * @author zoly
 */
public final class HeartBeatError extends Error {

  public HeartBeatError() {
  }

  public HeartBeatError(final String message) {
    super(message);
  }

  public HeartBeatError(final String message, final Throwable cause) {
    super(message, cause);
  }

  public HeartBeatError(final Throwable cause) {
    super(cause);
  }


}
