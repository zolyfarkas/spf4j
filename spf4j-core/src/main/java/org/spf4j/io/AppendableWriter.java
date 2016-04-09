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
 * this is a faster version of guava: CharStreams.asWriter
 * @author zoly
 */
@CleanupObligation
public final class AppendableWriter extends Writer {

  private final Appendable appendable;
  
  private final boolean flushable;
  
  private boolean closed;

  public AppendableWriter(final Appendable appendable) {
    this.appendable = appendable;
    this.flushable = appendable instanceof Flushable;
    this.closed = false;
  }

  @Override
  public void write(final char[] cbuf, final int off, final int len) throws IOException {
    checkNotClosed();
    /* Guava claims:
    It turns out that creating a new String is usually as fast,
    or faster than wrapping cbuf in a light-weight CharSequence.
    
    Since I am suspicious of claims that do not make much sense and most developers write rubbish benchmarks
    (Linus I start to be like you more and more),
    I wrote a JMH benchmark, and the results are as expected the opposite:
    
    Benchmark                                          Mode  Cnt      Score     Error  Units
    AppendableWriterBenchmark.guavaAppendable         thrpt   10  10731.940 ± 427.258  ops/s
    AppendableWriterBenchmark.spf4jAppendable         thrpt   10  17613.093 ± 344.769  ops/s
     
     Using a light weight wrapper is more than 50% faster!
    
     See AppendableWriterBenchmark for more detail...
    
    */
    appendable.append(CharBuffer.wrap(cbuf), off, off + len);
  }

  @Override
  public void write(final int c) throws IOException {
    checkNotClosed();
    appendable.append((char) c);
  }

  @Override
  public Writer append(final char c) throws IOException {
    checkNotClosed();
    appendable.append(c);
    return this;
  }

  @Override
  public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
    checkNotClosed();
    appendable.append(csq, start, end);
    return this;
  }

  @Override
  public Writer append(final CharSequence csq) throws IOException {
    checkNotClosed();
    appendable.append(csq);
    return this;
  }

  @Override
  public void write(final String str, final int off, final int len) throws IOException {
    checkNotClosed();
    appendable.append(str, off, off + len);
  }

  @Override
  public void write(final String str) throws IOException {
    appendable.append(str);
  }

  @Override
  public void write(final char[] cbuf) throws IOException {
    appendable.append(CharBuffer.wrap(cbuf));
  }

  @Override
  public void flush() throws IOException {
    checkNotClosed();
    if (flushable) {
      ((Flushable) appendable).flush();
    }
  }

  private void checkNotClosed() throws IOException {
    if (closed) {
      throw new IOException("Cannot write to closed writer " + this);
    }
  }
  
  @Override
  @DischargesObligation
  public void close() throws IOException {
    if (!closed) {
      flush();
      if (appendable instanceof Closeable) {
        ((Closeable) appendable).close();
      }
      closed = true;
    }
  }

  @Override
  public String toString() {
    return "AppendableWriter{" + "appendable=" + appendable + ", flushable=" + flushable + ", closed=" + closed + '}';
  }

}
