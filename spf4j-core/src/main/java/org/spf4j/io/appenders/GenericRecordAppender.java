
package org.spf4j.io.appenders;

import com.google.common.base.Charsets;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.JsonEncoder;
import org.spf4j.io.AppendableOutputStream;
import org.spf4j.io.ObjectAppender;

/**
 *
 * @author zoly
 */
public final class GenericRecordAppender implements ObjectAppender<GenericRecord> {
    

    @Override
    public void append(final GenericRecord object, final Appendable appendTo) throws IOException {
        try (AppendableOutputStream bos = new AppendableOutputStream(appendTo, Charsets.UTF_8)) {
            final Schema schema = object.getSchema();
            GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
            JsonEncoder jsonEncoder = SpecificRecordAppender.EF.jsonEncoder(schema, bos);
            writer.write(object, jsonEncoder);
            jsonEncoder.flush();
        }
    }
    
}
