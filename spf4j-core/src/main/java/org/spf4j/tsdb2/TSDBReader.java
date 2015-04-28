package org.spf4j.tsdb2;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
                new Schema.Parser().parse(header.getContentSchema().toString()));
    }


    @Nullable
    public synchronized Either<TableDef, DataBlock> read() throws IOException {
        if (bis.getReadBytes() >= size) {
            return null;
        }
        Object result = recordReader.read(null, decoder);
        if (result instanceof TableDef) {
            return Either.left((TableDef) result);
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
