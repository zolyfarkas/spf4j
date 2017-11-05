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
package org.spf4j.io;

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
