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
package org.spf4j.ssdump2;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillNotClose;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.spf4j.base.Methods;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.Method;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Converter {

  private Converter() {
  }

  public static String createLabeledSsdump2FileName(final String baseFileName, final String label) {
    try {
      String encoded = URLEncoder.encode(label, StandardCharsets.UTF_8.name());
      encoded = encoded.replace("_", "%5F");
      return baseFileName + '_' + encoded + ".ssdump2";
    } catch (UnsupportedEncodingException ex) {
      throw new IllegalArgumentException("Unable to encode: " + label, ex);
    }
  }

  public static String getLabelFromSsdump2FileName(final String fileName) {
    int spext = fileName.lastIndexOf(".ssdump2");
    if (spext < 0) {
      throw new IllegalArgumentException("Invalid ssdump2 file name: " + fileName);
    }
    int grsIdx = fileName.lastIndexOf('_', spext);
    try {
      if (grsIdx < 0) {
        return URLDecoder.decode(fileName.substring(0, spext), StandardCharsets.UTF_8.name());
      } else {
        return URLDecoder.decode(fileName.substring(grsIdx + 1, spext), StandardCharsets.UTF_8.name());
      }
    } catch (UnsupportedEncodingException ex) {
      throw new IllegalArgumentException("Invalid ssdump2 file name: " + fileName, ex);
    }
  }

  public static SampleNode convert(final Iterator<StackSampleElement> samples) {
    TIntObjectMap<SampleNode> index = new TIntObjectHashMap<>();
    while (samples.hasNext()) {
      StackSampleElement asmp = samples.next();
      SampleNode sn = new SampleNode(asmp.getCount());
      SampleNode parent = index.get(asmp.getParentId());
      if (parent != null) {
        Method m = asmp.getMethod();
        parent.put(m, sn);
      }
      index.put(asmp.getId(), sn);
    }
    return index.get(0);
  }

  public static void save(final File file, final SampleNode collected) throws IOException {
    try (OutputStream bos = newOutputStream(file)) {
      final SpecificDatumWriter<StackSampleElement> writer
              = new SpecificDatumWriter<>(StackSampleElement.getClassSchema());
      final BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(bos, null);
      Converters.convert(Methods.ROOT, collected,
              -1, 0, (StackSampleElement object) -> {
                try {
                  writer.write(object, encoder);
                } catch (IOException ex) {
                  throw new UncheckedIOException(ex);
                }
              });
      encoder.flush();
    }
  }

  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  public static SampleNode load(final File file) throws IOException {
    try (InputStream fis = newInputStream(file)) {
      return load(fis);
    }
  }

  public static SampleNode load(@WillNotClose final InputStream fis) throws IOException {
    try (MemorizingBufferedInputStream bis
            = new MemorizingBufferedInputStream(fis)) {
      final PushbackInputStream pis = new PushbackInputStream(bis);
      final SpecificDatumReader<StackSampleElement> reader =
              new SpecificDatumReader<>(StackSampleElement.getClassSchema());
      final BinaryDecoder decoder = DecoderFactory.get().directBinaryDecoder(pis, null);
      return convert(new Iterator<StackSampleElement>() {

        @Override
        public boolean hasNext() {
          try {
            int read = pis.read();
            pis.unread(read);
            return read >= 0;
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
        }

        @Override
        @SuppressFBWarnings
        public StackSampleElement next() {
          try {
            return reader.read(null, decoder);
          } catch (IOException ex) {
            NoSuchElementException e = new NoSuchElementException();
            e.addSuppressed(ex);
            throw e;
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      });
    }
  }

  public static void saveLabeledDumps(final File file, final Map<String, SampleNode> pcollected) throws IOException {
    try (OutputStream bos = newOutputStream(file)) {
      final SpecificDatumWriter<StackSampleElement> writer = new SpecificDatumWriter<>(StackSampleElement.SCHEMA$);
      final BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(bos, null);

      encoder.writeMapStart();
      final Map<String, SampleNode> collected = pcollected.entrySet().stream()
              .filter((e) -> e.getValue() != null)
              .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue()));
      encoder.setItemCount(collected.size());
      for (Map.Entry<String, SampleNode> entry : collected.entrySet()) {
        encoder.startItem();
        encoder.writeString(entry.getKey());
        encoder.writeArrayStart();
        Converters.convert(Methods.ROOT, entry.getValue(),
                -1, 0, (final StackSampleElement object) -> {
                  try {
                  encoder.setItemCount(1L);
                  encoder.startItem();
                  writer.write(object, encoder);
                  } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                  }
                });
        encoder.writeArrayEnd();
      }
      encoder.writeMapEnd();
      encoder.flush();
    }
  }

  private static OutputStream newOutputStream(final File file) throws IOException {
    OutputStream result = new BufferedOutputStream(
            Files.newOutputStream(file.toPath()));
    if (file.getName().endsWith(".gz")) {
      try {
        result = new GZIPOutputStream(result);
      } catch (IOException | RuntimeException ex) {
        result.close();
        throw ex;
      }
    }
    return result;
  }

  /**
   * Load samples forrm file containing multiple labeled stack samples.
   * @param file the ssdump3 file.
   * @return
   * @throws IOException
   */
  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  public static Map<String, SampleNode> loadLabeledDumps(final File file) throws IOException {
    try (InputStream bis = newInputStream(file)) {
      final SpecificDatumReader<StackSampleElement> reader = new SpecificDatumReader<>(StackSampleElement.SCHEMA$);
      final BinaryDecoder decoder = DecoderFactory.get().directBinaryDecoder(bis, null);
      long nrItems = decoder.readMapStart();
      StackSampleElement asmp = new StackSampleElement();
      Map<String, SampleNode> result = new HashMap<>((int) nrItems);
      while (nrItems > 0) {
        for (int i = 0; i < nrItems; i++) {
          String key = decoder.readString();
          TIntObjectMap<SampleNode> index = loadSamples(decoder, asmp, reader);
          result.put(key, index.get(0));
        }
        nrItems = decoder.mapNext();
      }
      return result;
    }
  }

  @SuppressFBWarnings("OCP_OVERLY_CONCRETE_PARAMETER")// it's a private method, don't care about being generic
  private static TIntObjectMap<SampleNode> loadSamples(final Decoder decoder,
          final StackSampleElement pasmp,
          final SpecificDatumReader<StackSampleElement> reader) throws IOException {
    TIntObjectMap<SampleNode> index = new TIntObjectHashMap<>();
    long nrArrayItems = decoder.readArrayStart();
    while (nrArrayItems > 0) {
      for (int j = 0; j < nrArrayItems; j++) {
        StackSampleElement asmp = reader.read(pasmp, decoder);
        SampleNode sn = new SampleNode(asmp.getCount());
        SampleNode parent = index.get(asmp.getParentId());
        if (parent != null) {
          Method readMethod = asmp.getMethod();
          Method m = new Method(readMethod.getDeclaringClass(), readMethod.getName());
          parent.put(m, sn);
        }
        index.put(asmp.getId(), sn);
      }
      nrArrayItems = decoder.arrayNext();
    }
    return index;
  }

  /**
   * Load the labels from a ssdump3 file.
   * @param file the ssdump3 file.
   * @throws IOException
   */
  public static void loadLabels(final File file, final Consumer<String> labalsConsumer) throws IOException {
    try (InputStream bis = newInputStream(file)) {
      final SpecificDatumReader<StackSampleElement> reader = new SpecificDatumReader<>(StackSampleElement.SCHEMA$);
      final BinaryDecoder decoder = DecoderFactory.get().directBinaryDecoder(bis, null);
      long nrItems = decoder.readMapStart();
      StackSampleElement asmp = new StackSampleElement();
      while (nrItems > 0) {
        for (int i = 0; i < nrItems; i++) {
          String key = decoder.readString();
          labalsConsumer.accept(key);
          skipDump(decoder, reader, asmp);
        }
        nrItems = decoder.mapNext();
      }
    }
  }

  @SuppressFBWarnings("OCP_OVERLY_CONCRETE_PARAMETER")// it's a private method, don't care about being generic
  private static void skipDump(final Decoder decoder,
          final SpecificDatumReader<StackSampleElement> reader,
          final StackSampleElement asmp) throws IOException {
    long nrArrayItems = decoder.readArrayStart();
    while (nrArrayItems > 0) {
      for (int j = 0; j < nrArrayItems; j++) {
        reader.read(asmp, decoder);
      }
      nrArrayItems = decoder.arrayNext();
    }
  }

 /**
   * Load samples forrm file containing multiple labeled stack samples.
   * @param file the ssdump3 file.
   * @return
   * @throws IOException
   */
  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  @Nullable
  public static SampleNode loadLabeledDump(final File file, final String label) throws IOException {
    try (InputStream bis = newInputStream(file)) {
      final SpecificDatumReader<StackSampleElement> reader = new SpecificDatumReader<>(StackSampleElement.SCHEMA$);
      final BinaryDecoder decoder = DecoderFactory.get().directBinaryDecoder(bis, null);
      long nrItems = decoder.readMapStart();
      StackSampleElement asmp = new StackSampleElement();
      while (nrItems > 0) {
        for (int i = 0; i < nrItems; i++) {
          String key = decoder.readString();
          if (label.equals(key)) {
            return loadSamples(decoder, asmp, reader).get(0);
          } else {
            skipDump(decoder, reader, asmp);
          }
        }
        nrItems = decoder.mapNext();
      }
      return null;
    }
  }

  private static InputStream newInputStream(final File file) throws IOException {
    InputStream result =  new BufferedInputStream(Files.newInputStream(file.toPath()));
    if (file.getName().endsWith(".gz")) {
      try {
        return new GZIPInputStream(result);
      } catch (IOException | RuntimeException ex) {
        result.close();
        throw ex;
      }
    } else {
      return result;
    }
  }


}
