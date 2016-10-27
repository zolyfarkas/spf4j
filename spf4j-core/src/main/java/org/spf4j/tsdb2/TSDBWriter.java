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
package org.spf4j.tsdb2;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Longs;
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
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.spf4j.base.Strings;
import org.spf4j.io.BufferedInputStream;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.recyclable.impl.ArraySuppliers;
import org.spf4j.tsdb2.avro.DataBlock;
import org.spf4j.tsdb2.avro.DataRow;
import org.spf4j.tsdb2.avro.Header;
import org.spf4j.tsdb2.avro.TableDef;

/**
 * Second generation Time-Series database format.
 * The linked list structure from first generation is dropped to reduce write overhead.
 *
 *
 * @author zoly
 */
public final class TSDBWriter implements Closeable, Flushable {

    public static final Schema FILE_RECORD_SCHEMA =
            Schema.createUnion(Arrays.asList(TableDef.SCHEMA$, DataBlock.SCHEMA$));


    private final File file;
    private final FileChannel channel;
    private final BinaryEncoder encoder;
    private final Header header;
    private final SpecificDatumWriter<Object> recordWriter = new SpecificDatumWriter<>(FILE_RECORD_SCHEMA);
    private final DataBlock writeBlock;
    private final int maxRowsPerBlock;
    private final  RandomAccessFile raf;

    static final byte[] MAGIC = Strings.toUtf8("TSDB2");
    private final ByteArrayBuilder bab;

    @CreatesObligation
    public TSDBWriter(final File file, final int maxRowsPerBlock,
            final String description, final boolean append) throws IOException {
        this.file = file;
        this.maxRowsPerBlock = maxRowsPerBlock;
        this.writeBlock = new DataBlock();
        this.writeBlock.baseTimestamp = System.currentTimeMillis();
        this.writeBlock.setValues(new ArrayList<DataRow>(maxRowsPerBlock));
        raf = new RandomAccessFile(file, "rw");
        bab = new ByteArrayBuilder(32768, ArraySuppliers.Bytes.JAVA_NEW);
        encoder = EncoderFactory.get().directBinaryEncoder(bab, null);
        channel = raf.getChannel();
        channel.lock();
        if (!append) {
            raf.setLength(0);
            channel.force(true);
        }
        if (raf.length() <= 0) {
            // new file or overwite, will write header;
            bab.write(MAGIC);
            toOutputStream(0, bab);
            header = Header.newBuilder()
                    .setContentSchema(FILE_RECORD_SCHEMA.toString())
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



    public synchronized long writeTableDef(final TableDef tableDef) throws IOException {
        final long position = raf.getFilePointer();
        bab.reset();
        tableDef.id = position;
        recordWriter.write(tableDef, encoder);
        encoder.flush();
        raf.write(bab.getBuffer(), 0, bab.size());
        return position;
    }


    public synchronized void writeDataRow(final long tableId, final long timestamp, final long... data)
            throws IOException {
        if (this.writeBlock.values.size() >= this.maxRowsPerBlock) {
            flush();
        }
        long baseTs = writeBlock.baseTimestamp;
        DataRow row = new DataRow();
        row.relTimeStamp = (int) (timestamp - baseTs);
        row.tableDefId = tableId;
        row.setData(Longs.asList(data));
        this.writeBlock.values.add(row);
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
     * @throws IOException
     */

    @Override
    public synchronized void flush() throws IOException {
        if (writeBlock.getValues().size() > 0) {
            bab.reset();
            this.recordWriter.write(writeBlock, this.encoder);
            encoder.flush();
            raf.write(bab.getBuffer(), 0, bab.size());
            channel.force(true);
            updateEOFPtrPointer();
            writeBlock.values.clear();
        }
        channel.force(true);
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
        return "TSDBWriter{" + "file=" + file + ", raf=" + raf + '}';
    }



}
