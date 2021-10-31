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
package org.spf4j.stackmonitor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.spf4j.base.DateTimeFormats;
import org.spf4j.base.avro.ApplicationStackSamples;
import org.spf4j.base.avro.Converters;

/**
 *
 * @author Zoltan Farkas
 */
public final class AvroProfilePersister implements ProfilePersister {

  private final DataFileWriter<ApplicationStackSamples> writer;

  private final boolean compress;

  private final Path targetFolder;

  private final String baseFileName;

  private final Path targetFile;

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public AvroProfilePersister(final Path targetFolder, final String baseFileName, final boolean compress)
          throws IOException {
      DataFileWriter<ApplicationStackSamples> dataWriter =
              new DataFileWriter<>(new SpecificDatumWriter<>(ApplicationStackSamples.class));
      this.compress = compress;
      this.targetFolder = targetFolder;
      this.baseFileName = baseFileName;
      if (compress) {
        try {
          Class.forName("com.github.luben.zstd.Zstd");
          dataWriter.setCodec(CodecFactory.zstandardCodec(-2, true));
        } catch (ClassNotFoundException | UnsatisfiedLinkError ex) {
          Logger.getLogger(AvroProfilePersister.class.getName()).fine("Fallback avro compress to deflate");
          dataWriter.setCodec(CodecFactory.deflateCodec(1));
        }
      }
      dataWriter.setMeta("appVersion", org.spf4j.base.Runtime.getAppVersionString());
      String fileName = baseFileName + ProfileFileFormat.SSP.getSuffix();
      targetFile = targetFolder.resolve(fileName);
      if (Files.isWritable(targetFile)) {
        Files.move(targetFile, targetFile.resolveSibling(targetFile.getFileName().toString()
                + ".backup." + DateTimeFormats.COMPACT_TS_FORMAT.format(Instant.now())));
      }
      dataWriter.create(ApplicationStackSamples.getClassSchema(), targetFile.toFile());
      this.writer = dataWriter;
  }


  @Override
  public boolean isCompressing() {
   return this.compress;
  }

  @Override
  public ProfilePersister withBaseFileName(final Path ptargetPath, final String pbaseFileName) throws IOException {
    return new AvroProfilePersister(ptargetPath, pbaseFileName, compress);
  }

  @Override
  public ProfilePersister witCompression(final boolean pcompress) throws IOException {
     return new AvroProfilePersister(this.targetFolder, this.baseFileName, pcompress);
  }

  @Override
  public Path persist(final Map<String, SampleNode> profile, final String tag,
          final Instant profileFrom, final Instant profileTo)
          throws IOException {
    if (profile.isEmpty()) {
      return this.targetFile;
    }
    for (Map.Entry<String, SampleNode> entry : profile.entrySet()) {
      this.writer.append(new ApplicationStackSamples(profileFrom, profileTo, tag,
            entry.getKey(), Converters.convert(entry.getValue())));
      this.writer.fSync();
    }
    return this.targetFile;
  }

  @Override
  public Path getTargetPath() {
    return this.targetFolder;
  }

  @Override
  public String getBaseFileNAme() {
    return this.baseFileName;
  }

  @Override
  public void close() throws IOException {
    this.writer.close();
  }

  public Path getTargetFile() {
    return targetFile;
  }

  @Override
  public String toString() {
    return "AvroProfilePersister{" + "compress=" + compress + ", targetFolder=" + targetFolder
            + ", baseFileName=" + baseFileName + ", targetFile=" + targetFile + '}';
  }

}
