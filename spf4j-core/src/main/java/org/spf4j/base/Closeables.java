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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public final class Closeables {

  private Closeables() {
  }

  @Nullable
  @CheckReturnValue
  public static Exception closeAll(final AutoCloseable... closeables) {
    return closeAll(null, closeables);
  }

  @Nullable
  @CheckReturnValue
  public static Exception closeAll(@Nullable final Exception propagate, final AutoCloseable... closeables) {
    Exception ex = propagate;
    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception ex1) {
        if (ex == null) {
          ex = ex1;
        } else {
          ex = Throwables.suppress(ex1, ex);
        }
      }
    }
    return ex;
  }

  @Nullable
  @CheckReturnValue
  public static IOException closeAll(final Closeable... closeables) {
    return closeAll(null, closeables);
  }

  @Nullable
  @CheckReturnValue
  public static IOException closeAll(@Nullable final Exception propagate, final Closeable... closeables) {
    IOException ex;
    if (propagate == null) {
      ex = null;
    } else if (propagate instanceof IOException) {
      ex = (IOException) propagate;
    } else {
      ex = new IOException(propagate);
    }
    for (Closeable closeable : closeables) {
      try {
        closeable.close();
      } catch (IOException ex1) {
        if (ex == null) {
          ex = ex1;
        } else {
          ex = Throwables.suppress(ex1, ex);
        }
      }
    }
    return ex;
  }

  @Nullable
  @CheckReturnValue
  public static Exception closeAll(final Iterable<? extends AutoCloseable> closeables) {
    return closeAll(null, closeables);
  }

  @Nullable
  @CheckReturnValue
  public static Exception closeAll(@Nullable final Exception propagate,
          final Iterable<? extends AutoCloseable> closeables) {
    Exception ex = propagate;
    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception ex1) {
        if (ex == null) {
          ex = ex1;
        } else {
          ex = Throwables.suppress(ex1, ex);
        }
      }
    }
    return ex;
  }

  @Nullable
  @CheckReturnValue
  public static IOException closeSelectorChannels(final Selector selector) {
    return closeSelectorChannels(null, selector);
  }

  @Nullable
  @CheckReturnValue
  public static IOException closeSelectorChannels(@Nullable final IOException propagate, final Selector selector) {
    IOException ex = propagate;
    for (SelectionKey key : selector.keys()) {
      SelectableChannel channel = key.channel();
      try {
        channel.close();
      } catch (IOException ex2) {
        if (ex == null) {
          ex = ex2;
        } else {
          ex = Throwables.suppress(ex, ex2);
        }
      }
    }
    return ex;
  }

}
