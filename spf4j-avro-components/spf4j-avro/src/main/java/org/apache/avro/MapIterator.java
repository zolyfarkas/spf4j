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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.spf4j.base.Pair;

/**
 * @author Zoltan Farkas
 */
public final class MapIterator<T> implements Iterator<Map.Entry<String, T>> {

  private final Decoder decoder;
  private final DatumReader<T> reader;
  private long l;

  public MapIterator(final Decoder decoder, final DatumReader<T> valueReader) {
    this.decoder = decoder;
    this.reader = valueReader;
    try {
      l = decoder.readMapStart();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public boolean hasNext() {
    return l > 0;
  }

  @Override
  public Map.Entry<String, T> next() {
    try {
      if (l <= 0) {
        throw new NoSuchElementException();
      }
      String key = decoder.readString();
      T read = reader.read(null, decoder);
      l--;
      if (l <= 0) {
        l = decoder.mapNext();
      }
      return Pair.of(key, read);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String toString() {
    return "ArrayIterator{" + "decoder=" + decoder + ", reader=" + reader + ", l=" + l + '}';
  }

}
