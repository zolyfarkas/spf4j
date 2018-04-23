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

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Utility class that limits the nr of characters written to a particular Appender.
 * if nr of characters exceed the limit the character above the limit + entire message are written to
 * the specified file.
 * THe destination appender will contain the chars that fit the limit + a reference to the overflow file
 * if overflow happened.
 * @author zoly
 */
@Beta
@CleanupObligation
public final class AppendableLimiterWithOverflow implements Appendable, Closeable {


  private final int directWriteLimit;
  private final int limit;
  private final Appendable destination;
  private int count;
  private Writer overflowWriter;
  private final CharSequence destinationSuffix;
  private final StringBuilder buffer;
  private StringBuilder asideBuffer;
  private final OverflowSupplier owflSupplier;

  /**
   * provide the overflow.
   */
  public  interface OverflowSupplier {
    /**
     * @return - a string that you can use to reference the overflow. (file name, url...)
     * this string is used as a suffix for the appender that is being limited.
     */
    CharSequence getOverflowReference();

    /**
     * @return - a writer to write the overflow.
     */
    Writer getOverflowWriter() throws IOException;
  }



  public AppendableLimiterWithOverflow(final int limit, final File overflowFile,
          final CharSequence destinationSuffix, final Charset characterSet, final Appendable destination) {
    this(limit, destination, new OverflowSupplier() {
      @Override
      public CharSequence getOverflowReference() {
        String path = overflowFile.getPath();
        StringBuilder sb = new StringBuilder(path.length() + destinationSuffix.length());
        sb.append(destinationSuffix);
        sb.append(path);
        return sb;
      }

      @Override
      public Writer getOverflowWriter() throws IOException {
        return new BufferedWriter(
              new OutputStreamWriter(Files.newOutputStream(overflowFile.toPath()), characterSet));
      }
    });
  }

  public AppendableLimiterWithOverflow(final int limit, final Appendable destination,
          final OverflowSupplier owflSupplier) {
    this.limit = limit;
    this.destinationSuffix = owflSupplier.getOverflowReference();
    final int refSize = destinationSuffix.length();
    this.directWriteLimit = limit - refSize;
    if (this.directWriteLimit < 0) {
      throw new IllegalArgumentException("Limit too small " + limit + " should be at least " + refSize);
    }
    this.destination = destination;
    this.count = 0;
    if (destination instanceof CharSequence) {
      buffer = null;
    } else {
      buffer = new StringBuilder(limit);
    }
    this.overflowWriter = null;
    this.asideBuffer = null;
    this.owflSupplier = owflSupplier;
  }


  @Override
  public Appendable append(final CharSequence csq) throws IOException {
    if (csq == null) {
      append("null", 0, "null".length());
    } else {
      append(csq, 0, csq.length());
    }
    return this;
  }

  @Override
  public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
    int nrChars = end - start;
    int charsToWrite = Math.min(directWriteLimit - count, nrChars);
    int dwe = start + charsToWrite;
    if (charsToWrite > 0) {
      destination.append(csq, start, dwe);
      if (buffer != null) {
        buffer.append(csq, start, charsToWrite);
      }
      count += charsToWrite;
    }
    int charsToWriteAside = Math.min(limit - count, end - dwe);
    if (charsToWriteAside > 0) {
      if (asideBuffer == null) {
        asideBuffer = new StringBuilder(limit - directWriteLimit);
      }
      asideBuffer.append(csq, dwe, dwe + charsToWriteAside);
      count += charsToWriteAside;
    }
    if (charsToWrite + charsToWriteAside < end) {
      createOverflowIfNeeded();
      overflowWriter.append(csq, charsToWrite + charsToWriteAside, end);
    }
    return this;
  }

  @Override
  public Appendable append(final char c) throws IOException {
    int charsToWrite = Math.min(directWriteLimit - count, 1);
    if (charsToWrite > 0) {
      destination.append(c);
      if (buffer != null) {
        buffer.append(c);
      }
      count++;
    } else {
      int charsToWriteAside = Math.min(limit - count, 1);
      if (charsToWriteAside > 0) {
        if (asideBuffer == null) {
          asideBuffer = new StringBuilder(limit - directWriteLimit);
        }
        asideBuffer.append(c);
        count++;
      } else {
        createOverflowIfNeeded();
        overflowWriter.append(c);
      }
    }
    return this;
  }


  public void createOverflowIfNeeded() throws IOException {
    if (count >= limit && overflowWriter == null) {
      overflowWriter = owflSupplier.getOverflowWriter();
      if (buffer != null) {
        overflowWriter.append(buffer);
      } else {
        CharSequence cs = (CharSequence) destination;
        final int l = cs.length();
        overflowWriter.append(cs, l - directWriteLimit, l);
      }
      overflowWriter.append(asideBuffer);
      asideBuffer = null;
      destination.append(destinationSuffix);
    }
  }


  @Override
  @DischargesObligation
  public void close() throws IOException {
    if (overflowWriter != null) {
      overflowWriter.close();
    } else if (asideBuffer != null) {
      destination.append(asideBuffer);
    }
  }

  @Override
  public String toString() {
    return "AppendableLimiterWithOverflow{" + "directWriteLimit=" + directWriteLimit
            + ", limit=" + limit + ", destination=" + destination + ", count=" + count
            + ", overflowWriter=" + overflowWriter + ", destinationSuffix=" + destinationSuffix
            + ", buffer=" + buffer + ", overflow=" + owflSupplier + ", asideBuffer=" + asideBuffer + '}';
  }


}
