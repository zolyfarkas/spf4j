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

  private final Object sync;

  public LazyOutputStreamWrapper(final Supplier<OutputStream> osSuplier) {
    this.osSuplier = osSuplier;
    this.sync = new Object();
  }

  private OutputStream getWrapped() {
    OutputStream os = wrapped;
    if (os == null) {
      synchronized (sync) {
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
