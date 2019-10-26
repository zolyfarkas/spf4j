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
import javax.annotation.concurrent.ThreadSafe;
import org.spf4j.base.ArrayWriter;

@ThreadSafe
public final class InterceptableArrayWriter<T> implements ArrayWriter<T> {

  private final ArrayWriter<T> wrapped;

  private final ArrayEventHandler<T> handler;

  private boolean isFirst;

  public InterceptableArrayWriter(final ArrayWriter<T> wrapped, final ArrayEventHandler<T> handler) {
    this.wrapped = wrapped;
    this.handler = handler;
    this.isFirst = true;
  }

  @Override
  public synchronized void write(final T t) throws IOException {
    if (isFirst) {
      handler.first(t);
      isFirst = false;
    }
    wrapped.write(t);
 }

  @Override
  public synchronized void accept(final T t) {
    if (isFirst) {
      handler.first(t);
      isFirst = false;
    }
    wrapped.accept(t);
  }

  @Override
  public synchronized void close() throws IOException {
    if (isFirst) {
      handler.empty();
    }
    wrapped.close();
  }

  @Override
  public void flush() throws IOException {
    wrapped.flush();
  }

  @Override
  public String toString() {
    return "InterceptableArrayWriter{" + "wrapped=" + wrapped
            + ", handler=" + handler + ", isFirst=" + isFirst + '}';
  }

}
