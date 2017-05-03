
package org.spf4j.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A utility class that allows you to delay any writes made in the constructor of a particular writer (headers).
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class LazyOutputStreamWrapper extends OutputStream {


  private volatile OutputStream wrapped;

  private final Supplier<OutputStream> osSuplier;

  public LazyOutputStreamWrapper(final Supplier<OutputStream> osSuplier) {
    this.osSuplier = osSuplier;
  }

  private OutputStream getWrapped() {
    OutputStream os = wrapped;
    if (os == null) {
      synchronized (osSuplier) {
        os = wrapped;
        if (os == null) {
          os = osSuplier.get();
          wrapped = os;
        }
      }
    }
    return os;
  }

  @Override
  public void write(final int b) throws IOException {
    getWrapped().write(b);
  }

  @Override
  public void close() throws IOException {
    getWrapped().close();
  }

  @Override
  public void flush() throws IOException {
    getWrapped().flush();
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    getWrapped().write(b, off, len);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    getWrapped().write(b);
  }

  @Override
  public String toString() {
    return "LazyOutputStreamWrapper{" + "wrapped=" + wrapped + ", osSuplier=" + osSuplier + '}';
  }

}
