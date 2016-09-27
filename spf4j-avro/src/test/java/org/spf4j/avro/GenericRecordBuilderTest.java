
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
    try (GenericRecordBuilder builder = new GenericRecordBuilder(schema)) {
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
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testMoreGenericRecordCreation() throws InstantiationException, IllegalAccessException {
    Schema reuse = SchemaBuilder.builder().record("Reuse").fields().requiredString("field").endRecord();
    Schema record = SchemaBuilder.builder().record("TestRecord").fields()
            .requiredInt("number")
            .name("record").type(reuse).noDefault()
            .endRecord();
    Schema record2 = SchemaBuilder.builder().record("TestRecord2").fields()
            .requiredBoolean("isThere")
            .name("record").type(reuse).noDefault()
            .endRecord();
    try (GenericRecordBuilder builder = new GenericRecordBuilder(record, record2)) {
      Class<? extends SpecificRecordBase> clasz = builder.getClass(record);
      Class<? extends SpecificRecordBase> clasz2 = builder.getClass(record);
      Assert.assertSame(clasz, clasz2);
      SpecificRecordBase myRecord = clasz.newInstance();
      myRecord.put("number", 35);
      Assert.assertEquals(35, (int) myRecord.get("number"));
      Class<? extends SpecificRecordBase> aClass = builder.getClass(record2);
      GenericRecord r2 = aClass.newInstance();
      System.out.println(r2);
    }
  }

}
