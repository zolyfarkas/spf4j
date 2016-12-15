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
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.spf4j.base.Strings;
import org.spf4j.io.BufferedInputStream;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.recyclable.impl.ArraySuppliers;
import org.spf4j.tsdb2.avro.Header;

/**
 * File format to sequentially write Avro records.
 * Format:
 *
 * AVROZ[ptr LastFrameEnd][Header][Snappy Frame]....[Snappy Frame]
 *
 * Snappy frame detail: https://github.com/google/snappy/blob/master/framing_format.txt
 *
 * @author zoly
 */
@Beta
public final class AvroFileWriter<T extends IndexedRecord> implements Closeable, Flushable {

  private final File file;
  private final FileChannel channel;
  private final BinaryEncoder encoder;
  private final Header header;
  private final GenericDatumWriter<Object> recordWriter;
  private final List<T> writeBlocks;
  private final int maxRowsPerBlock;
  private final RandomAccessFile raf;

  static final byte[] MAGIC = Strings.toUtf8("AVROZ");
  private final ByteArrayBuilder bab;

  @CreatesObligation
  public AvroFileWriter(final File file, final Schema schema, final Class<?> objectType,
          final int maxRowsPerBlock, final String description, final boolean append) throws IOException {
    if (SpecificRecord.class.isAssignableFrom(objectType)) {
      recordWriter = new SpecificDatumWriter<>(schema);
    } else {
      recordWriter = new GenericDatumWriter<>(schema);
    }
    this.file = file;
    this.maxRowsPerBlock = maxRowsPerBlock;
    this.writeBlocks = new ArrayList<>(maxRowsPerBlock);
    raf = new RandomAccessFile(file, "rw");
    bab = new ByteArrayBuilder(32768, ArraySuppliers.Bytes.JAVA_NEW);
    encoder = EncoderFactory.get().directBinaryEncoder(bab, null);
    channel = raf.getChannel();
    channel.lock();
    if (!append) { // overwrite file.
      raf.setLength(0);
      channel.force(true);
    }
    if (raf.length() <= 0) {
      // new file or overwite, will write header;
      bab.write(MAGIC);
      toOutputStream(0, bab);
      header = Header.newBuilder()
              .setContentSchema(schema.toString())
              .setDescription(description)
              .build();
      SpecificDatumWriter<Header> headerWriter = new SpecificDatumWriter<>(Header.SCHEMA$);
      headerWriter.write(header, encoder);
      encoder.flush();
      byte[] buffer = bab.getBuffer();
      final int size = bab.size();
      toByteArray(size, buffer, MAGIC.length);
      raf.write(buffer, 0, size);
    } else {
      if (description != null) {
        throw new IllegalArgumentException("Providing description when appending is not allowed for " + file);
      }

      try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
              DataInputStream dis = new DataInputStream(bis)) {
        validateType(dis);
        long size = dis.readLong();
        SpecificDatumReader<Header> reader = new SpecificDatumReader<>(Header.getClassSchema());
        BinaryDecoder directBinaryDecoder = DecoderFactory.get().directBinaryDecoder(dis, null);
        header = reader.read(null, directBinaryDecoder);
        raf.seek(size);
      }
    }
  }

  static void validateType(final InputStream dis) throws IOException {
    byte[] readMagic = new byte[MAGIC.length];
    ByteStreams.readFully(dis, readMagic);
    if (!Arrays.equals(MAGIC, readMagic)) {
      throw new IOException("wrong file type, magic is " + Arrays.toString(readMagic));
    }
  }

  public synchronized void write(final T record) throws IOException {
    writeBlocks.add(record);
    if (writeBlocks.size() >= maxRowsPerBlock) {
      flush();
    }
  }


  @Override
  public synchronized void close() throws IOException {
    try (RandomAccessFile f = raf) {
      flush();
    }
  }

  public File getFile() {
    return file;
  }

  public static void toByteArray(final long pvalue, final byte[] bytes, final int idx) {
    long value = pvalue;
    for (int i = idx + 7; i >= idx; i--) {
      bytes[i] = (byte) (value & 0xffL);
      value >>= 8;
    }
  }

  public static void toOutputStream(final long pvalue, final OutputStream os) throws IOException {
    long value = pvalue;
    for (int i = 7; i >= 0; i--) {
      os.write((byte) (value & 0xffL));
      value >>= 8;
    }
  }

  /**
   * Commits the data to disk.
   *
   * @throws IOException
   */
  @Override
  public synchronized void flush() throws IOException {
    int nrRecords = writeBlocks.size();
    if (nrRecords > 0) {
      bab.reset();
      for (T obj : writeBlocks) {
        this.recordWriter.write(obj, this.encoder);
      }
      int bsize = bab.size();

      raf.writeInt(nrRecords);
      raf.writeInt(bsize);
      raf.write(bab.getBuffer(), 0, bsize);
      encoder.flush();
      channel.force(true);
      updateEOFPtrPointer();
      writeBlocks.clear();
      channel.force(true);
    }
  }

  private void updateEOFPtrPointer() throws IOException {
    long filePointer = raf.getFilePointer();
    raf.seek(MAGIC.length);
    raf.writeLong(filePointer);
    raf.seek(filePointer);
  }

  public Header getHeader() {
    return header;
  }

  @Override
  public String toString() {
    return "AVROFileWriter{" + "file=" + file + ", raf=" + raf + '}';
  }

}
