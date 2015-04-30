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

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.spf4j.base.Either;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.tsdb2.avro.DataBlock;
import org.spf4j.tsdb2.avro.Header;
import org.spf4j.tsdb2.avro.TableDef;

/**
 *
 * @author zoly
 */
public final  class TSDBReader implements Closeable {

    private final MemorizingBufferedInputStream bis;
    private final Header header;
    private final long size;
    private final BinaryDecoder decoder;
    private final SpecificDatumReader<Object> recordReader;

    public TSDBReader(final File file, final int bufferSize) throws IOException {
        final FileInputStream fis = new FileInputStream(file);
        bis = new MemorizingBufferedInputStream(fis);
        SpecificDatumReader<Header> reader = new SpecificDatumReader<>(Header.getClassSchema());
        decoder = DecoderFactory.get().directBinaryDecoder(bis, null);
        TSDBWriter.validateType(bis);
        DataInputStream dis = new DataInputStream(bis);
        size = dis.readLong();
        header = reader.read(null, decoder);
        recordReader = new SpecificDatumReader<>(
                new Schema.Parser().parse(header.getContentSchema()),
                Schema.createUnion(Arrays.asList(TableDef.SCHEMA$, DataBlock.SCHEMA$)));
    }


    @Nullable
    public synchronized Either<TableDef, DataBlock> read() throws IOException {
        final long position = bis.getReadBytes();
        if (position >= size) {
            return null;
        }
        Object result = recordReader.read(null, decoder);
        if (result instanceof TableDef) {
            final TableDef td = (TableDef) result;
            if (position != td.id) {
                throw new IOException("Table Id should be equal with file position " + position + ", " + td.id);
            }
            return Either.left(td);
        } else {
            return Either.right((DataBlock) result);
        }
    }


    @Override
    public synchronized void close() throws IOException {
        bis.close();
    }

    public long getSize() {
        return size;
    }

    public Header getHeader() {
        return header;
    }

}
