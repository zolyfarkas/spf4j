package org.spf4j.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;

/**
 * Utility class to adapt a Appendable to a Writer.
 * @author zoly
 */
@CleanupObligation
public final class AppendableWriter extends Writer {

  private final Appendable appendable;
  
  private final boolean flushable;

  public AppendableWriter(final Appendable appendable) {
    this.appendable = appendable;
    this.flushable = appendable instanceof Flushable;
  }

  @Override
  public void write(final char[] cbuf, final int off, final int len) throws IOException {
    appendable.append(CharBuffer.wrap(cbuf), off, len);
  }

  @Override
  public void write(final int c) throws IOException {
    appendable.append((char) c);
  }

  @Override
  public Writer append(final char c) throws IOException {
    appendable.append(c);
    return this;
  }

  @Override
  public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
      appendable.append(csq, start, end);
      return this;
  }

  @Override
  public Writer append(final CharSequence csq) throws IOException {
    appendable.append(csq);
    return this;
  }


  @Override
  public void flush() throws IOException {
    if (flushable) {
      ((Flushable) appendable).flush();
    }
  }

  @Override
  @DischargesObligation
  public void close() throws IOException {
    flush();
    if (appendable instanceof Closeable) {
      ((Closeable) appendable).close();
    }
  }

  @Override
  public String toString() {
    return "AppendableWriter{" + "appendable=" + appendable + '}';
  }

}
