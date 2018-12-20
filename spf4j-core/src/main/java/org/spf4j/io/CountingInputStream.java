
/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.spf4j.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;

/**
 * An {@link InputStream} that counts the number of bytes read.
 * initial count can be provided
 */
public final class CountingInputStream extends FilterInputStream {

  private long count;
  private long mark = -1;

  /**
   * Wraps another input stream, counting the number of bytes read.
   *
   * @param in the input stream to be wrapped
   */
  public CountingInputStream(@Nonnull final InputStream in, final long count) {
    super(in);
    this.count = count;
  }

  /** Returns the number of bytes read. */
  public synchronized long getCount() {
    return count;
  }

  @Override
  public synchronized int read() throws IOException {
    int result = in.read();
    if (result != -1) {
      count++;
    }
    return result;
  }

  @Override
  public  synchronized int read(final byte[] b, final int off, final int len) throws IOException {
    int result = in.read(b, off, len);
    if (result != -1) {
      count += result;
    }
    return result;
  }

  @Override
  public synchronized long skip(final long n) throws IOException {
    long result = in.skip(n);
    count += result;
    return result;
  }

  @Override
  public synchronized void mark(final int readlimit) {
    in.mark(readlimit);
    mark = count;
    // it's okay to mark even if mark isn't supported, as reset won't work
  }

  @Override
  public synchronized void reset() throws IOException {
    if (!in.markSupported()) {
      throw new IOException("Mark not supported by " + in);
    }
    if (mark == -1) {
      throw new IOException("Mark not set for " + this);
    }

    in.reset();
    count = mark;
  }

  @Override
  public String toString() {
    return "CountingInputStream{" + "count=" + count + ", mark=" + mark + '}';
  }


}
