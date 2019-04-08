/*
 * Copyright 2019 SPF4J.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Zoltan Farkas
 */
public final class DebugInputStream extends InputStream {

  private final InputStream source;

  private final File destination;

  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public DebugInputStream(final InputStream wrapped, final File destinationDir) throws IOException {
    this.destination = File.createTempFile("stream", ".tmp", destinationDir);
    Path toPath = destination.toPath();
    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(toPath))) {
      Streams.copy(wrapped, os);
    }
    source = new BufferedInputStream(Files.newInputStream(toPath));
  }

  @Override
  public int read() throws IOException {
    return source.read();
  }

  @Override
  public boolean markSupported() {
    return source.markSupported();
  }

  @Override
  public synchronized void reset() throws IOException {
    source.reset();
  }

  @Override
  public synchronized void mark(final int readlimit) {
    source.mark(readlimit);
  }

  @Override
  public void close() throws IOException {
    source.close();
  }

  @Override
  public int available() throws IOException {
    return source.available();
  }

  @Override
  public long skip(final long n) throws IOException {
    return source.skip(n);
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    return source.read(b, off, len);
  }

  @Override
  public int read(final byte[] b) throws IOException {
    return source.read();
  }

  @Override
  public String toString() {
    return "DebugInputStream{" + "source=" + source + ", destination=" + destination + '}';
  }

}
