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
package org.spf4j.tsdb2;

import org.spf4j.io.CountingInputStream;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Longs;
import com.sun.nio.file.SensitivityWatchEventModifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.spf4j.base.Either;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Handler;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.tsdb2.avro.DataBlock;
import org.spf4j.tsdb2.avro.Header;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
public final class TSDBReader implements Closeable {

  private static final boolean CORUPTION_LENIENT = Boolean.getBoolean("spf4j.tsdb2.lenientRead");

  private static final Schema R_SCHEMA = Schema.createUnion(Arrays.asList(TableDef.SCHEMA$, DataBlock.SCHEMA$));

  private CountingInputStream bis;
  private final Header header;
  private long size;
  private BinaryDecoder decoder;
  private final SpecificDatumReader<Object> recordReader;
  private RandomAccessFile raf;
  private final File file;
  private final Path filePath;
  private volatile boolean watch;
  private final int bufferSize;
  private final SeekableByteChannel byteChannel;

  public TSDBReader(final File file, final int bufferSize) throws IOException {
    this(file, bufferSize, 0L);
  }


  public TSDBReader(final File file, final int bufferSize, final long from) throws IOException {
    this.file = file;
    this.filePath = file.toPath();
    this.bufferSize = bufferSize;
    this.byteChannel = Files.newByteChannel(filePath);
    resetStream(0);
    SpecificDatumReader<Header> reader = new SpecificDatumReader<>(Header.getClassSchema());
    TSDBWriter.validateType(bis);
    byte[] buff = new byte[8];
    ByteStreams.readFully(bis, buff);
    size = Longs.fromByteArray(buff);
    header = reader.read(null, decoder);
    recordReader = new SpecificDatumReader<>(
            new Schema.Parser().parse(header.getContentSchema()), R_SCHEMA);
    if (from > 0L) {
      resetStream(from);
    }
  }

  private void resetStream(final long position) throws IOException {
    byteChannel.position(position);
    bis = new CountingInputStream(new MemorizingBufferedInputStream(Channels.newInputStream(byteChannel),
            bufferSize), position);
    decoder = DecoderFactory.get().directBinaryDecoder(bis, decoder);
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
    raf.seek(TSDBWriter.MAGIC.length);
    long old = size;
    size = raf.readLong();
    if (size != old) {
      resetStream(bis.getCount());
      return true;
    } else {
      return false;
    }
  }

  @Nullable
  public synchronized Either<TableDef, DataBlock> read() throws IOException {
    final long position = bis.getCount();
    if (position >= size) {
      return null;
    }
    Object result;
    try {
      result = recordReader.read(null, decoder);
    } catch (IOException | RuntimeException ex) {
      if (CORUPTION_LENIENT) {
        return null;
      } else {
        throw new IOException("Error reading tsdb file at " + position + ", this= " + this, ex);
      }
    }
    if (result instanceof TableDef) {
      final TableDef td = (TableDef) result;
      long tdId = td.getId();
      if (position != tdId) {
        throw new IOException("Table Id should be equal with file position " + position + ", " + tdId);
      }
      return Either.left(td);
    } else {
      return Either.right((DataBlock) result);
    }
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

  public void stopWatching() {
    watch = false;
  }

  public synchronized <E extends Exception> Future<Void> bgWatch(
          final Handler<Either<TableDef, DataBlock>, E> handler,
          final EventSensitivity es) {
    return bgWatch(handler, es, ExecutionContexts.getContextDeadlineNanos());
  }

  public synchronized <E extends Exception> Future<Void> bgWatch(
          final Handler<Either<TableDef, DataBlock>, E> handler,
          final EventSensitivity es, final long timeout, final TimeUnit unit) {
    return bgWatch(handler, es, TimeSource.nanoTime() + unit.toNanos(timeout));
  }

  //CHECKSTYLE:OFF
  public synchronized <E extends Exception> Future<Void> bgWatch(
          final Handler<Either<TableDef, DataBlock>, E> handler,
          final EventSensitivity es, final long deadlineNanos) {
    //CHECKSTYLE:ON
    return DefaultExecutor.INSTANCE.submit(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        watch(handler, es);
        return null;
      }
    });
  }

  public enum EventSensitivity {
    HIGH, MEDIUM, LOW
  }

  public <E extends Exception> void watch(final Handler<Either<TableDef, DataBlock>, E> handler,
          final EventSensitivity es) throws IOException, InterruptedException, E {
    watch(handler, es, ExecutionContexts.getContextDeadlineNanos());
  }

  public <E extends Exception> void watch(final Handler<Either<TableDef, DataBlock>, E> handler,
          final EventSensitivity es, final long timeout, final TimeUnit unit)
          throws IOException, InterruptedException, E {
    watch(handler, es, TimeSource.nanoTime() + unit.toNanos(timeout));
  }

  //CHECKSTYLE:OFF
  @SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
  public <E extends Exception> void watch(final Handler<Either<TableDef, DataBlock>, E> handler,
          final EventSensitivity es, final long deadlineNanos)
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
      readAll(handler, deadlineNanos);
      do {
        long tNanos = deadlineNanos - TimeSource.nanoTime();
        if (tNanos <= 0) {
          break;
        }
        WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
        if (key == null) {
          if (reReadSize()) {
            readAll(handler, deadlineNanos);
          }
          continue;
        }
        if (!key.isValid()) {
          key.cancel();
          break;
        }
        if (!key.pollEvents().isEmpty() && reReadSize()) {
          readAll(handler, deadlineNanos);
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
  public synchronized <E extends Exception> void readAll(final Handler<Either<TableDef, DataBlock>, E> handler,
          final long deadlineNanos)
          throws IOException, E {
    //CHECKSTYLE:ON
    Either<TableDef, DataBlock> data;
    while ((data = read()) != null) {
      handler.handle(data, deadlineNanos);
    }
  }

  public synchronized void readAll(final Consumer<Either<TableDef, DataBlock>> consumer)
          throws IOException {
    Either<TableDef, DataBlock> data;
    while ((data = read()) != null) {
      consumer.accept(data);
    }
  }

  @Override
  public String toString() {
    return "TSDBReader{" + "size=" + size + ", raf=" + raf + ", file=" + file + '}';
  }

}
