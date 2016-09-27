package org.spf4j.avro;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class GenericRecordBenchmark {


  private static final Schema SCHEMA;
  private static final Class<? extends SpecificRecordBase> CLASZ;
  static {
    try {
      SCHEMA = new Schema.Parser().parse(
              Resources.toString(Resources.getResource("SchemaBuilder.avsc"), Charsets.US_ASCII));
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
    CLASZ = new GenericRecordBuilder(SCHEMA).getClass(SCHEMA);
  }


  @Benchmark
  public GenericRecord testSpf4jGenericRecordNew() throws InstantiationException, IllegalAccessException {
    return CLASZ.newInstance();
  }

  @Benchmark
  public GenericRecord testAvroGenericRecordNew() throws IOException {
    return new GenericData.Record(SCHEMA);
  }

  @Benchmark
  public GenericRecord testSpf4jGenericRecordNewSetGet() throws InstantiationException, IllegalAccessException {
    GenericRecord record =  CLASZ.newInstance();
    record.put("requiredBoolean", true);
    assert (Boolean) record.get("requiredBoolean");
    record.put(1, true);
    assert (Boolean) record.get(1);
    return record;
  }

  @Benchmark
  public GenericRecord testAvroGenericRecordNewSetGet() throws IOException {
    GenericRecord record = new GenericData.Record(SCHEMA);
    record.put("requiredBoolean", true);
    assert (Boolean) record.get("requiredBoolean");
    record.put(1, true);
    assert (Boolean) record.get(1);
    return record;
  }



}
