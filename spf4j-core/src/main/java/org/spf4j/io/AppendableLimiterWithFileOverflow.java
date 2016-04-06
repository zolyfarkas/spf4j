
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
 *
 * @author zoly
 */
@Beta
@CleanupObligation
public final class AppendableLimiterWithFileOverflow implements Appendable, Closeable {

  
  private final int limit;
  private final Appendable destination;
  private int count;
  private Writer overflowWriter;
  private final String destinationSuffix;
  private final StringBuilder buffer;
  private final File overflowFile;
  
  public AppendableLimiterWithFileOverflow(final int limit, final File overflowFile,
          final String destinationSuffix, final Appendable destination) {
    final int refSize = destinationSuffix.length() + overflowFile.getPath().length();
    this.limit = limit - refSize;
    if (this.limit < 0) {
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
  }

  
  @Override
  public Appendable append(final CharSequence csq) throws IOException {
    append(csq, 0, csq.length());
    return this;
  }

  @Override
  public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
    int nrChars = end - start;
    int charsToWrite = Math.min(limit - count, nrChars);
    if (charsToWrite > 0) {
      destination.append(csq, start, start + charsToWrite);
      count += charsToWrite;
    }
    createOverflowIfNeeded();
    if (charsToWrite < end) {
      overflowWriter.append(csq, charsToWrite, end);
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
        overflowWriter.append(cs, l - limit, l);
      }
      destination.append(destinationSuffix);
      destination.append(overflowFile.getPath());
    }
  }

  @Override
  public Appendable append(final char c) throws IOException {
    int charsToWrite = Math.min(limit - count, 1);
    if (charsToWrite > 0) {
      destination.append(c);
      count++;
    }
    createOverflowIfNeeded();
    if (charsToWrite == 0) {
      overflowWriter.append(c);
    }
    return this;
    
  }

  @Override
  @DischargesObligation
  public void close() throws IOException {
    if (overflowWriter != null) {
      overflowWriter.close();
    }
  }

  @Override
  public String toString() {
    return "AppendableLimiterWithFileOverflow{" + "limit=" + limit + ", destination=" + destination
            + ", count=" + count + ", overflowWriter=" + overflowWriter + ", destinationSuffix="
            + destinationSuffix + ", buffer=" + buffer + ", overflowFile=" + overflowFile + '}';
  }
  
}
