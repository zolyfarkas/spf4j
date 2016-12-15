/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.io.avro;

import com.google.common.annotations.Beta;
import com.sun.nio.file.SensitivityWatchEventModifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.io.FSWatchEventSensitivity;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.tsdb2.avro.Header;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
@Beta
public final class AvroFileReader<T extends IndexedRecord> implements Closeable {

  private final MemorizingBufferedInputStream bis;
  private final Header header;
  private long size;
  private final BinaryDecoder decoder;
  private final GenericDatumReader<Object> recordReader;
  private RandomAccessFile raf;
  private final File file;

  public AvroFileReader(final File file, final Schema schema, final Class<T> recordType, final int bufferSize)
          throws IOException {
    this.file = file;
    final FileInputStream fis = new FileInputStream(file);
    bis = new MemorizingBufferedInputStream(fis);
    SpecificDatumReader<Header> reader = new SpecificDatumReader<>(Header.getClassSchema());
    decoder = DecoderFactory.get().directBinaryDecoder(bis, null);
    AvroFileWriter.validateType(bis);
    DataInputStream dis = new DataInputStream(bis);
    size = dis.readLong();
    header = reader.read(null, decoder);
    if (SpecificRecord.class.isAssignableFrom(recordType)) {
      recordReader = new SpecificDatumReader<>(
              new Schema.Parser().parse(header.getContentSchema()), schema);
    } else {
      recordReader = new GenericDatumReader(
              new Schema.Parser().parse(header.getContentSchema()), schema);
    }
  }

  /**
   * method useful when implementing tailing.
   *
   * @return true if size changed.
   * @throws IOException
   */
  public synchronized boolean reReadSize() throws IOException {
    if (raf == null) {
      raf = new RandomAccessFile(file, "r");
    }
    raf.seek(AvroFileWriter.MAGIC.length);
    long old = size;
    size = raf.readLong();
    return size != old;
  }

  @Nullable
  public synchronized T read() throws IOException {
    final long position = bis.getReadBytes();
    if (position >= size) {
      return null;
    }
    return (T) recordReader.read(null, decoder);
  }

  @Override
  public synchronized void close() throws IOException {
    try (InputStream is = bis) {
      if (raf != null) {
        raf.close();
      }
    }
  }

  public synchronized long getSize() {
    return size;
  }

  public Header getHeader() {
    return header;
  }

  private volatile boolean watch;

  public void stopWatching() {
    watch = false;
  }

  //CHECKSTYLE:OFF
  public synchronized <E extends Exception> Future<Void> bgWatch(
          final SowAndSubscribeHandler<T, E> handler,
          final FSWatchEventSensitivity es) {
    //CHECKSTYLE:ON
    return DefaultExecutor.INSTANCE.submit(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        sowAndSubscribe(handler, es);
        return null;
      }
    });
  }

  public interface SowHandler<T, E extends Exception> {

    void handle(T object) throws E;

  }

  public interface SowAndSubscribeHandler<T, E extends Exception> extends SowHandler {

    void sowEnd();

  }

  //CHECKSTYLE:OFF
  @SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
  public <E extends Exception> void sowAndSubscribe(final SowAndSubscribeHandler<T, E> handler,
          final FSWatchEventSensitivity es)
          throws IOException, InterruptedException, E {
    //CHECKSTYLE:ON
    synchronized (this) {
      if (watch) {
        throw new IllegalStateException("File is already watched " + file);
      }
      watch = true;
    }
    SensitivityWatchEventModifier sensitivity;
    switch (es) {
      case LOW:
        sensitivity = SensitivityWatchEventModifier.LOW;
        break;
      case MEDIUM:
        sensitivity = SensitivityWatchEventModifier.MEDIUM;
        break;
      case HIGH:
        sensitivity = SensitivityWatchEventModifier.HIGH;
        break;
      default:
        throw new UnsupportedOperationException("Unsupported sensitivity " + es);
    }
    final Path path = file.getParentFile().toPath();
    try (WatchService watchService = path.getFileSystem().newWatchService()) {
      path.register(watchService, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.OVERFLOW
      }, sensitivity);
      readAll(handler);
      handler.sowEnd();
      do {
        WatchKey key = watchService.poll(1000, TimeUnit.MILLISECONDS);
        if (key == null) {
          if (reReadSize()) {
            readAll(handler);
          }
          continue;
        }
        if (!key.isValid()) {
          key.cancel();
          break;
        }
        if (!key.pollEvents().isEmpty()) {
          if (reReadSize()) {
            readAll(handler);
          }
        }
        if (!key.reset()) {
          key.cancel();
          break;
        }
      } while (watch);
    } finally {
      watch = false;
    }
  }

  //CHECKSTYLE:OFF
  public synchronized <E extends Exception> void readAll(final SowHandler<T, E> handler)
          throws IOException, E {
    //CHECKSTYLE:ON
    T data;
    while ((data = read()) != null) {
      handler.handle(data);
    }
  }

  @Override
  public String toString() {
    return "AvroFileReader{" + "size=" + size + ", raf=" + raf + ", file=" + file + '}';
  }

}
