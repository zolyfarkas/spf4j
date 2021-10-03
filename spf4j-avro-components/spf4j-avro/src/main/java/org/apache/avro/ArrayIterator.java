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
package org.apache.avro;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;

/**
 * @author Zoltan Farkas
 */
public final class ArrayIterator<T> implements Iterator<T> {

  private final Decoder decoder;
  private final DatumReader<T> reader;
  private long l;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ArrayIterator(final Decoder decoder, final DatumReader<T> elementReader) {
    this.decoder = decoder;
    this.reader = elementReader;
    try {
      l = decoder.readArrayStart();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public boolean hasNext() {
    return l > 0;
  }

  @Override
  public T next() {
    try {
      if (l <= 0) {
        throw new NoSuchElementException();
      }
      T read = reader.read(null, decoder);
      l--;
      if (l <= 0) {
        l = decoder.arrayNext();
      }
      return read;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String toString() {
    return "ArrayIterator{" + "decoder=" + decoder + ", reader=" + reader + ", l=" + l + '}';
  }

}
