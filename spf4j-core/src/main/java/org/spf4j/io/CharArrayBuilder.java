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

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import org.spf4j.recyclable.SizedRecyclingSupplier;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * Utility class to avoid replicating character arrays for no good reason.
 *
 * @author zoly
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
@CleanupObligation
public final class CharArrayBuilder extends Writer {

  /**
   * The maximum size of array to allocate. Some VMs reserve some header words in an array. Attempts to allocate larger
   * arrays may result in OutOfMemoryError: Requested array size exceeds VM limit
   */
  private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

  /**
   * The buffer where data is stored.
   */
  private char[] buf;

  /**
   * The number of valid bytes in the buffer.
   */
  private int count;

  private final SizedRecyclingSupplier<char[]> arraySupplier;

  public CharArrayBuilder() {
    this(256, ArraySuppliers.Chars.TL_SUPPLIER);
  }

  public CharArrayBuilder(final int size) {
    this(size, ArraySuppliers.Chars.TL_SUPPLIER);
  }

  public synchronized char[] getBuffer() {
    return buf;
  }

  /**
   * Creates a new byte array output stream, with a buffer capacity of the specified size, in bytes.
   *
   * @param size the initial size.
   * @exception IllegalArgumentException if size is negative.
   */
  public CharArrayBuilder(final int size, final SizedRecyclingSupplier<char[]> arraySupplier) {
    if (size < 0) {
      throw new IllegalArgumentException("Negative initial size: "
              + size);
    }
    this.arraySupplier = arraySupplier;
    if (arraySupplier == null) {
      if (size == 0) {
        buf = org.spf4j.base.Arrays.EMPTY_CHAR_ARRAY;
      } else {
        buf = new char[size];
      }
    } else {
      buf = arraySupplier.get(size);
    }
  }

  /**
   * Increases the capacity if necessary to ensure that it can hold at least the number of elements specified by the
   * minimum capacity argument.
   *
   * @param minCapacity the desired minimum capacity
   * @throws OutOfMemoryError if {@code minCapacity < 0}. This is interpreted as a request for the unsatisfiably large
   * capacity {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}.
   */
  private void ensureCapacity(final int minCapacity) {
    // overflow-conscious code
    if (minCapacity - buf.length > 0) {
      grow(minCapacity);
    }
  }

  /**
   * Increases the capacity to ensure that it can hold at least the number of elements specified by the minimum capacity
   * argument.
   *
   * @param minCapacity the desired minimum capacity
   */
  private void grow(final int minCapacity) {
    // overflow-conscious code
    int oldCapacity = buf.length;
    int newCapacity = oldCapacity << 1;
    if (newCapacity - minCapacity < 0) {
      newCapacity = minCapacity;
    }
    if (newCapacity - MAX_ARRAY_SIZE > 0) {
      newCapacity = hugeCapacity(minCapacity);
    }
    if (arraySupplier == null) {
      buf = Arrays.copyOf(buf, newCapacity);
    } else {
      char[] old = buf;
      buf = arraySupplier.get(newCapacity);
      System.arraycopy(old, 0, buf, 0, old.length);
      arraySupplier.recycle(old);
    }
  }

  private static int hugeCapacity(final int minCapacity) {
    if (minCapacity < 0) { // overflow
      throw new OutOfMemoryError();
    }
    return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
  }

  @Override
  public void write(final char[] b) {
    write(b, 0, b.length);
  }

  /**
   * Writes the specified byte to this byte array output stream.
   *
   * @param b the byte to be written.
   */
  @Override
  public synchronized void write(final int b) {
    int cp1 = count + 1;
    ensureCapacity(cp1);
    buf[count] = (char) b;
    count = cp1;
  }

  /**
   * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code> to this byte array
   * output stream.
   *
   * @param b the data.
   * @param off the start offset in the data.
   * @param len the number of bytes to write.
   */
  @Override
  public synchronized void write(final char[] b, final int off, final int len) {
    if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) - b.length > 0)) {
      throw new IndexOutOfBoundsException();
    }
    int cpl = count + len;
    ensureCapacity(cpl);
    System.arraycopy(b, off, buf, count, len);
    count = cpl;
  }

  /**
   * Writes the complete contents of this char array output stream to the specified output stream argument, as if by
   * calling the output stream's write method using <code>out.write(buf, 0, count)</code>.
   *
   * @param out the output stream to which to write the data.
   * @exception IOException if an I/O error occurs.
   */
  public synchronized void writeTo(final Writer out) throws IOException {
    out.write(buf, 0, count);
  }

  public synchronized void readFrom(final Reader in) throws IOException {
    do {
      ensureCapacity(count + 8192);
      int nr = in.read(buf, count, 8192);
      if (nr < 0) {
        break;
      }
      count += nr;
    } while (true);
  }

  /**
   * Resets the <code>count</code> field of this byte array output stream to zero, so that all currently accumulated
   * output in the output stream is discarded. The output stream can be used again, reusing the already allocated buffer
   * space.
   *
   * @see java.io.ByteArrayInputStream#count
   */
  public synchronized void reset() {
    count = 0;
  }

  public synchronized void resetCountTo(final int pos) {
    count = pos;
  }

  /**
   * Creates a newly allocated byte array. Its size is the current size of this output stream and the valid contents of
   * the buffer have been copied into it.
   *
   * @return the current contents of this output stream, as a byte array.
   * @see java.io.ByteArrayOutputStream#size()
   */
  public synchronized char[] toByteArray() {
    return Arrays.copyOf(buf, count);
  }

  /**
   * Returns the current size of the buffer.
   *
   * @return the value of the <code>count</code> field, which is the number of valid bytes in this output stream.
   * @see java.io.ByteArrayOutputStream#count
   */
  public synchronized int size() {
    return count;
  }

  /**
   * Converts the buffer's contents into a string decoding bytes using the platform's default character set. The length
   * of the new <tt>String</tt>
   * is a function of the character set, and hence may not be equal to the size of the buffer.
   *
   * <p>
   * This method always replaces malformed-input and unmappable-character sequences with the default replacement string
   * for the platform's default character set. The {@linkplain java.nio.charset.CharsetDecoder} class should be used
   * when more control over the decoding process is required.
   *
   * @return String decoded from the buffer's contents.
   */
  @Override
  public synchronized String toString() {
    if (buf == null) {
      return "Closed CharArrayBuilder";
    } else {
      return new String(buf, 0, count);
    }
  }

  /**
   * Closing a <tt>ByteArrayOutputStream</tt> will likely recycle the underlying buffer, using the builder after close
   * is NOT advised
   */
  @DischargesObligation
  @Override
  public synchronized void close() {
    if (arraySupplier != null) {
      arraySupplier.recycle(buf);
      buf = null;
    }
  }

  @Override
  public void flush() {
    // nothing to flush
  }

}
