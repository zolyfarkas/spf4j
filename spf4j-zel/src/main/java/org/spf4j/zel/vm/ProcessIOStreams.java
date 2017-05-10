
package org.spf4j.zel.vm;

import java.io.InputStream;
import java.io.PrintStream;
import javax.annotation.concurrent.Immutable;

/**
 * @author zoly
 */
@Immutable
public final class ProcessIOStreams implements ProcessIO {

  public static final ProcessIO DEFAULT =  new ProcessIOStreams(System.in, System.out, System.err);

  /**
   * Standard Input
   */
  private final transient InputStream in;

  /**
   * Standard Output
   */
  private final transient PrintStream out;

  /**
   * Standard Error Output
   */
  private final transient PrintStream err;

  public ProcessIOStreams(final InputStream in, final PrintStream out, final PrintStream err) {
    this.in = in;
    this.out = out;
    this.err = err;
  }

  @Override
  public InputStream getIn() {
    return in;
  }

  @Override
  public PrintStream getOut() {
    return out;
  }

  @Override
  public PrintStream getErr() {
    return err;
  }

  @Override
  public String toString() {
    return "ProcessIOStreams{" + "in=" + in + ", out=" + out + ", err=" + err + '}';
  }

}
