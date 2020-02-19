/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.perf.impl.ms.tsdb;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Longs;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TLongHashSet;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.perf.MeasurementStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.jmx.JmxExport;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.tsdb2.TableDefs;
import org.spf4j.tsdb2.avro.Aggregation;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.Observation;
import org.spf4j.tsdb2.avro.TableDef;
import org.spf4j.tsdb2.avro.Type;

/**
 *
 * @author zoly
 */
@ThreadSafe
public final class AvroMeasurementStore
        implements MeasurementStore {

  private CodecFactory codecFact;

  private DataFileWriter<TableDef> infoWriter;

  private DataFileWriter<Observation> dataWriter;

  private Path infoFile;

  private Path dataFile;

  private long ids;

  private final long timeRef;


  public AvroMeasurementStore(final Path destinationPath, final String fileNameBase) throws IOException {
    this(destinationPath, fileNameBase,
            Boolean.parseBoolean(System.getProperty("spf4j.perf.avro.snappyEnable", "true")));
  }

  public AvroMeasurementStore(final Path destinationPath, final String fileNameBase, final boolean snappyCompress)
  throws IOException {
    if (snappyCompress) {
      try {
        Class.forName("org.xerial.snappy.Snappy");
        codecFact = CodecFactory.snappyCodec();
      } catch (ClassNotFoundException ex) {
        Logger.getLogger(AvroMeasurementStore.class.getName())
                .info("Snappy compression not available for metrics store");
        codecFact = null;
      }
    } else {
      codecFact = null;
    }
    AvroFileInfo<TableDef> info = initWriter(fileNameBase, destinationPath, true, TableDef.class);
    this.infoFile = info.getFilePath();
    this.infoWriter = info.getFileWriter();
    AvroFileInfo<Observation> data = initWriter(fileNameBase, destinationPath, false, Observation.class);
    this.dataFile = data.getFilePath();
    this.dataWriter = data.getFileWriter();
    timeRef = data.getFileEpoch();
   }

  private <T extends SpecificRecord>
       AvroFileInfo<T> initWriter(final String fileNameBase,
          final Path destinationPath,
          final boolean countEntries,
          final Class<T> clasz) throws IOException {
    DataFileWriter<T> writer = new DataFileWriter<>(new SpecificDatumWriter<>(clasz));
    if (codecFact != null) {
      writer.setCodec(codecFact);
    }
    long epoch = System.currentTimeMillis();
    writer.setMeta("timeRef", epoch);
    String fileName = fileNameBase + '_' + clasz.getSimpleName().toLowerCase(Locale.US) +  ".avro";
    Path file = destinationPath.resolve(fileName);
    long initNrRecords;
    if (Files.isWritable(file)) {
      try (DataFileStream<T> streamReader = new DataFileStream<>(Files.newInputStream(file),
              new SpecificDatumReader<>(clasz))) {
        if (countEntries) {
          long count = 0L;
          while (streamReader.hasNext()) {
            count += streamReader.getBlockCount();
            streamReader.nextBlock();
          }
          initNrRecords = count;
        } else {
          initNrRecords = -1L;
        }
        epoch = streamReader.getMetaLong("timeRef");
      }
      writer = writer.appendTo(file.toFile());
    } else {
      try {
        writer.create(clasz.newInstance().getSchema(), file.toFile());
      } catch (InstantiationException | IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
      initNrRecords = 0L;
    }
    return new AvroFileInfo<>(file, writer, epoch, initNrRecords);
  }


  @Override
  public long alocateMeasurements(final MeasurementsInfo measurement,
          final int sampleTimeMillis) throws IOException {
    int numberOfMeasurements = measurement.getNumberOfMeasurements();
    List<ColumnDef> columns = new ArrayList<>(numberOfMeasurements);
    for (int i = 0; i < numberOfMeasurements; i++) {
      String mname = measurement.getMeasurementName(i);
      String unit = measurement.getMeasurementUnit(i);
      Aggregation aggregation = measurement.getMeasurementAggregation(i);
      ColumnDef cd = new ColumnDef(mname, Type.LONG, unit, "", aggregation);
      columns.add(cd);
    }
    synchronized (infoWriter) {
      long id = ids++;
      infoWriter.append(new TableDef(id,
              measurement.getMeasuredEntity().toString(),
              "", columns, sampleTimeMillis, measurement.getMeasurementType()));
      return id;
    }
  }

  public static <T extends SpecificRecord> long getNrRecords(final Path avroFile, final Class<T> clasz)
          throws IOException {
    try (DataFileStream<T> streamReader = new DataFileStream<>(Files.newInputStream(avroFile),
            new SpecificDatumReader<>(clasz))) {
      long count = 0L;
      while (streamReader.hasNext()) {
        count += streamReader.getBlockCount();
        streamReader.nextBlock();
      }
      return count;
    }
  }


  @Override
  public void saveMeasurements(final long tableId,
          final long timeStampMillis, final long... measurements)
          throws IOException {
    synchronized (dataWriter) {
      dataWriter.append(new Observation(timeStampMillis - timeRef, tableId, Longs.asList(measurements)));
    }
  }

  @Override
  public void close() throws IOException {
    synchronized (infoWriter) {
      infoWriter.close();
    }
    synchronized (dataWriter) {
      dataWriter.close();
    }
  }

  @JmxExport(description = "flush out buffers")
  @Override
  public void flush() throws IOException {
    synchronized (infoWriter) {
      infoWriter.flush();
    }
    synchronized (dataWriter) {
      dataWriter.flush();
    }
  }

  @Override
  public Set<String> getMeasurements() throws IOException {
    Set<String> result = new THashSet<>();
    try (DataFileStream<TableDef> stream = new DataFileStream<TableDef>(Files.newInputStream(infoFile),
            new SpecificDatumReader<>(TableDef.class))) {
      for (TableDef td : stream) {
        result.add(td.getName());
      }
    }
    return result;
  }

  @Override
  @Nullable
  public AvroCloseableIterable<TimeSeriesRecord> getMeasurementData(final String measurement,
          final Instant from, final Instant to) throws IOException {
    TLongSet mids = new TLongHashSet(4, 0.7f, -1L);
    TableDef def = null;
    try (DataFileStream<TableDef> stream = new DataFileStream<TableDef>(Files.newInputStream(infoFile),
            new SpecificDatumReader<>(TableDef.class))) {
      for (TableDef td : stream) {
        if (measurement.equals(td.getName())) {
          mids.add(td.getId());
          if (def != null && !td.getColumns().equals(def.getColumns())) {
            throw new IllegalStateException("Invalid measurement table redefinition " + def + " != " + td);
          }
          def = td;
        }
      }
    }
    if (def == null) {
      return null;
    }
    Schema schema = TableDefs.createSchema(def);
    DataFileStream<Observation> stream = new DataFileStream<Observation>(Files.newInputStream(dataFile),
            new SpecificDatumReader<>(Observation.class));
    long fileTimeRef = stream.getMetaLong("timeRef");
    long fromMs = from.toEpochMilli();
    long toMs = to.toEpochMilli();
    Iterable<Observation> filtered = Iterables.filter(stream, (Observation row) -> {
      long ts = fileTimeRef + row.getRelTimeStamp();
      return ts >= fromMs && ts <= toMs && mids.contains(row.getTableDefId());
    });
    return AvroCloseableIterable.from(Iterables.transform(filtered,
            (obs) -> TableDefs.toRecord(schema, fileTimeRef, obs)),
            stream, schema);
  }


  @Override
  public boolean readable() {
    return true;
  }

  @Override
  @Nullable
  public Schema getMeasurementSchema(final String measurement) throws IOException {
    TableDef def = null;
    try (DataFileStream<TableDef> stream = new DataFileStream<TableDef>(Files.newInputStream(infoFile),
            new SpecificDatumReader<>(TableDef.class))) {
      for (TableDef td : stream) {
        if (measurement.equals(td.getName())) {
          if (def != null && !td.getColumns().equals(def.getColumns())) {
            throw new IllegalStateException("Invalid measurement table redefinition " + def + " != " + td);
          }
          def = td;
        }
      }
    }
    if (def == null) {
      return null;
    }
    return TableDefs.createSchema(def);
  }

  public Path getInfoFile() {
    return infoFile;
  }

  public Path getDataFile() {
    return dataFile;
  }


  @Override
  public String toString() {
    return "AvroMeasurementStore{" + "codecFact=" + codecFact + ", infoWriter=" + infoWriter
            + ", dataWriter=" + dataWriter + ", infoFile=" + infoFile + ", dataFile=" + dataFile
            + ", ids=" + ids + ", timeRef=" + timeRef + '}';
  }



}
