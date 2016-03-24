
package org.spf4j.io.appenders;

import com.google.common.base.Charsets;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.spf4j.io.AppendableOutputStream;
import org.spf4j.io.ObjectAppender;

/**
 *
 * @author zoly
 */
public final class SpecificRecordAppender implements ObjectAppender<SpecificRecord> {
    
    static final EncoderFactory EF = new EncoderFactory();

    @Override
    public void append(final SpecificRecord object, final Appendable appendTo) throws IOException {
        try (AppendableOutputStream bos = new AppendableOutputStream(appendTo, Charsets.UTF_8)) {
            final Schema schema = object.getSchema();
            SpecificDatumWriter<SpecificRecord> writer = new SpecificDatumWriter<>(schema);
            JsonEncoder jsonEncoder = EF.jsonEncoder(schema, bos);
            writer.write(object, jsonEncoder);
            jsonEncoder.flush();
        }
    }
    
}
