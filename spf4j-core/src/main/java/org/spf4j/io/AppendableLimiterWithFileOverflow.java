
package org.spf4j.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

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
public final class AppendableLimiterWithFileOverflow implements Appendable, Closeable {

  
  private final int directWriteLimit;
  private final int limit;
  private final Appendable destination;
  private int count;
  private Writer overflowWriter;
  private final String destinationSuffix;
  private final StringBuilder buffer;
  private final File overflowFile;
  private StringBuilder asideBuffer;
  
  public AppendableLimiterWithFileOverflow(final int limit, final File overflowFile,
          final String destinationSuffix, final Appendable destination) {
    this.limit = limit;
    final int refSize = destinationSuffix.length() + overflowFile.getPath().length();
    this.directWriteLimit = limit - refSize;
    if (this.directWriteLimit < 0) {
      throw new IllegalArgumentException("Limit too small " + limit + " should be at least " + refSize);
    }
    this.destination = destination;
    this.count = 0;
    this.destinationSuffix = destinationSuffix;
    if (destination instanceof CharSequence) {
      buffer = null;
    } else {
      buffer = new StringBuilder(limit);
    }
    this.overflowFile = overflowFile;
    this.overflowWriter = null;
    this.asideBuffer = null;
  }

  
  @Override
  public Appendable append(final CharSequence csq) throws IOException {
    append(csq, 0, csq.length());
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
      overflowWriter = new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(overflowFile, false), Charsets.UTF_8));
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
      destination.append(overflowFile.getPath());
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
    return "AppendableLimiterWithFileOverflow{" + "directWriteLimit=" + directWriteLimit
            + ", limit=" + limit + ", destination=" + destination + ", count=" + count
            + ", overflowWriter=" + overflowWriter + ", destinationSuffix=" + destinationSuffix
            + ", buffer=" + buffer + ", overflowFile=" + overflowFile + ", asideBuffer=" + asideBuffer + '}';
  }


}
