/*
 * Copyright 2021 SPF4J.
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
package org.spf4j.stackmonitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.specific.SpecificDatumReader;
import org.spf4j.base.avro.ApplicationStackSamples;
import org.spf4j.ssdump2.Converter;

/**
 *
 * @author Zoltan Farkas
 */
public final class AvroStackSampleSupplier implements StackSampleSupplier {

  private final Instant from;

  private final Instant to;

  private final Path file;

  private final SpecificDatumReader<ApplicationStackSamples> reader;

  public AvroStackSampleSupplier(final Path file) throws IOException {
    this.file = file;
    this.reader = new SpecificDatumReader<>(ApplicationStackSamples.class);
    Instant lfrom = Instant.MIN;
    Instant lto = Instant.MAX;
    try (DataFileStream<ApplicationStackSamples> stream = new DataFileStream<>(Files.newInputStream(file), reader)) {

      if (stream.hasNext()) {
        ApplicationStackSamples samples = stream.next();
        lfrom = samples.getCollectedFrom();
        lto = samples.getCollectedTo();
      }
      while (stream.hasNext()) {
        ApplicationStackSamples samples = stream.next();
        lto = samples.getCollectedTo();
      }
      this.from = lfrom;
      this.to = lto;
    }
  }

  @Override
  public Instant getMin() {
    return from;
  }

  @Override
  public Instant getMax() {
    return to;
  }

  @Override
  public ProfileMetaData getMetaData(final Instant pfrom, final Instant pto) throws IOException {
    Set<String> contexts = new HashSet<>();
    Set<String> tags = new HashSet<>();
    try (DataFileStream<ApplicationStackSamples> stream = new DataFileStream<>(Files.newInputStream(file), reader)) {
      while (stream.hasNext()) {
        ApplicationStackSamples samples = stream.next();
        Instant sampleFrom = samples.getCollectedFrom();
        Instant sampleTo = samples.getCollectedTo();
        if ((sampleFrom.compareTo(sampleTo) == 0
                && sampleFrom.compareTo(pfrom) >= 0 && sampleFrom.compareTo(pfrom) <= 0)
                || (sampleFrom.isBefore(pto) && sampleTo.isAfter(pfrom))) {
          contexts.add(samples.getContext());
          tags.add(samples.getTag());
        }
      }
    }
    return new ProfileMetaData(contexts, tags);
  }

  @Override
  public SampleNode getSamples(final String context, final String tag,
          final Instant pfrom, final Instant pto) throws IOException {
    SampleNode result = null;
    try (DataFileStream<ApplicationStackSamples> stream = new DataFileStream<>(Files.newInputStream(file), reader)) {
      while (stream.hasNext()) {
        ApplicationStackSamples samples = stream.next();
        Instant sampleFrom = samples.getCollectedFrom();
        Instant sampleTo = samples.getCollectedTo();
        if (((sampleFrom.compareTo(sampleTo) == 0
                && sampleFrom.compareTo(pfrom) >= 0 && sampleFrom.compareTo(pfrom) <= 0)
                || (sampleFrom.isBefore(pto) && sampleTo.isAfter(pfrom)))
                && (tag == null || samples.getTag().equals(tag))
                && (context == null || samples.getContext().equals(context))) {
          SampleNode currentSamples = Converter.convert(samples.getStackSamples().iterator());
          if (result == null) {
            result = currentSamples;
          } else if (currentSamples != null) {
            result.add(currentSamples);
          }
        }
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return "AvroStackSampleSupplier{" + "from=" + from + ", to=" + to + ", file=" + file + '}';
  }

}
