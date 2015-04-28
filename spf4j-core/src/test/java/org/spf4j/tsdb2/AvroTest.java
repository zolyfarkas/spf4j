
package org.spf4j.tsdb2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import junit.framework.Assert;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.Test;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.tsdb2.avro.DataBlock;

/**
 *
 * @author zoly
 */
public class AvroTest {


    @Test
    public void testRw() throws IOException {
        DataBlock data = DataBlock.newBuilder()
                .setBaseTimestamp(0).setValues(Collections.EMPTY_LIST).build();
        try (ByteArrayBuilder bab = new ByteArrayBuilder()) {
            SpecificDatumWriter<DataBlock> writer = new SpecificDatumWriter<>(data.getSchema());
            final BinaryEncoder directBinaryEncoder = EncoderFactory.get().directBinaryEncoder(bab, null);
            writer.write(data, directBinaryEncoder);
            directBinaryEncoder.flush();
            ByteArrayInputStream bis = new ByteArrayInputStream(bab.getBuffer(), 0, bab.size());
            SpecificDatumReader<DataBlock> reader = new SpecificDatumReader<>(data.getSchema());
            BinaryDecoder directBinaryDecoder = DecoderFactory.get().directBinaryDecoder(bis, null);
            DataBlock read = reader.read(null, directBinaryDecoder);
            Assert.assertEquals(read, data);
        }
    }

}
