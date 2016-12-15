package org.spf4j.io.avro;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.Test;

/**
 * @author zoly
 */
public class AvroFileReaderTest {


  @Test
  public void testWriteRead() throws IOException {
    Schema schema = SchemaBuilder.builder().record("TestRecord").fields()
            .requiredDouble("number")
            .requiredString("someString").endRecord();
    File file = File.createTempFile("temp", ".tavro");

    AvroFileWriter<GenericRecord> writer = new AvroFileWriter<>(file, schema, GenericRecord.class, 10,
            "test records", true);
    GenericData.Record record = new GenericData.Record(schema);
    record.put("number", 3.14);
    record.put("someString", "test string");
    writer.write(record);
    GenericRecord record2 = new GenericData.Record(record, true);
    record2.put("number", 1);
    writer.write(record2);
    writer.close();

    AvroFileReader<GenericRecord> reader = new AvroFileReader<>(file, schema, GenericRecord.class, 10000);
    GenericRecord read = reader.read();
    Assert.assertEquals(3.14, (double) read.get("number"), 0.0001);
    GenericRecord read2 = reader.read();
    Assert.assertEquals(1, (double) read2.get("number"), 0.0001);
    reader.close();

  }

}
