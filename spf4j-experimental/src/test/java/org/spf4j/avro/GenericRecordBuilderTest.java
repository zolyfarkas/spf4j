
package org.spf4j.avro;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecordBase;
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
    GenericRecordBuilder builder = new GenericRecordBuilder();
    builder.addSchema(schema);
    Class<? extends SpecificRecordBase> clasz = builder.getClass(schema);
    GenericRecord record = clasz.newInstance();
    record.put("requiredBoolean", true);
    Schema fieldSchema = schema.getField("optionalRecord").schema().getTypes().get(1);
    Class<? extends SpecificRecordBase> nestedRecordClass = builder.getClass(fieldSchema);
    SpecificRecordBase nr = nestedRecordClass.newInstance();
    nr.put("nestedRequiredBoolean", true);
    record.put("optionalRecord", nr);
    System.out.println(record);
  }

}
