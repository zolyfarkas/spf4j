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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * need to extend java.io.Closeable so that jaxrs does not interfere.
 * @author Zoltan Farkas
 */
@CleanupObligation
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

  @DischargesObligation
  void close();

  static <T> CloseableIterator<T> from(final Iterator<T> it) {
    return from(it, () -> { });
  }

  static <T> CloseableIterator<T> from(final Iterator<T> it, final AutoCloseable close) {
    return new CloseableIterator<T>() {
      @Override
      @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
      public void close() {
        try {
          close.close();
        } catch (RuntimeException | Error e) {
          throw e;
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public T next() {
        return it.next();
      }

      @Override
      public void remove() {
        it.remove();
      }

    };
  }

  default Stream<T> toStreamRemaining() {
    return toStreamRemaining(false);
  }

  default Stream<T> toStreamRemaining(final boolean parallel) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, 0), parallel).onClose(() -> this.close());
  }

}
