
package org.spf4j.avro;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.junit.Assert;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class GenericRecordBuilderTest {


  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testGenericRecordCreation() throws IOException, Exception {
    String schemaStr = Resources.toString(Resources.getResource("SchemaBuilder.avsc"), Charsets.US_ASCII);
    Schema schema = new Schema.Parser().parse(schemaStr);
    try (GenericRecordBuilder builder = new GenericRecordBuilder()) {
      builder.addSchema(schema);
      Class<? extends SpecificRecordBase> clasz = builder.getClass(schema);
      GenericRecord record = clasz.newInstance();
      record.put("requiredBoolean", Boolean.TRUE);
      Schema fieldSchema = schema.getField("optionalRecord").schema().getTypes().get(1);
      Class<? extends SpecificRecordBase> nestedRecordClass = builder.getClass(fieldSchema);
      SpecificRecordBase nr = nestedRecordClass.newInstance();
      nr.put("nestedRequiredBoolean", Boolean.TRUE);
      record.put("optionalRecord", nr);
      Assert.assertEquals(nr, record.get("optionalRecord"));
      System.out.println(record);
    }
  }

  @Test
  public void testMoreGenericRecordCreation() throws InstantiationException, IllegalAccessException {
    Schema record = SchemaBuilder.builder().record("TestRecord").fields().requiredInt("number").endRecord();
    try (GenericRecordBuilder builder = new GenericRecordBuilder()) {
      builder.addSchema(record);
      Class<? extends SpecificRecordBase> clasz = builder.getClass(record);
      SpecificRecordBase myRecord = clasz.newInstance();
      myRecord.put("number", 35);
      Assert.assertEquals(35, (int) myRecord.get("number"));
    }
  }

}
