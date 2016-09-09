
package org.spf4j.beans;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class GenericRecordBuilderTest {


  @Test
  public void testGenericRecordCreation() throws IOException, Exception {
    String schemaStr = Resources.toString(Resources.getResource("SchemaBuilder.avsc"), Charsets.US_ASCII);
    Schema schema = new Schema.Parser().parse(schemaStr);
    Class<? extends GenericRecord> clasz = GenericRecordBuilder.createClass(schema);
    GenericRecord record = clasz.newInstance();
    record.put("requiredBoolean", true);
    System.out.println(record);
  }

}
