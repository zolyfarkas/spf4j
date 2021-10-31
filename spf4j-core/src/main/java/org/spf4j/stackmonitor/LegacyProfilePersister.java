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
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import javax.annotation.Nullable;
import org.spf4j.base.CharSequences;
import org.spf4j.base.DateTimeFormats;
import org.spf4j.ssdump2.Converter;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public final class LegacyProfilePersister implements ProfilePersister {

  private static final String GZIP_SUFFIX = ".gz";

  private final String baseFileName;

  private final File targetFolder;

  private final boolean compress;

  private final ProfileFileFormat format;

  private final boolean explicitFileName;

  public LegacyProfilePersister(final Path targetFolder, final String baseFileName, final boolean compress) {
    this(targetFolder.toFile(), baseFileName, compress);
  }

  private LegacyProfilePersister(final File targetFolder, final String baseFileName, final boolean compress) {
    CharSequences.validatedFileName(baseFileName);
    this.targetFolder = targetFolder;
    this.baseFileName = baseFileName;
    DumpType dt = getRequestedFileType(baseFileName);
    if (dt == null) {
      this.compress = compress;
      this.format = null;
      this.explicitFileName = false;
    } else {
      this.compress = dt.isIsGzipped();
      this.format = dt.getFormat();
      this.explicitFileName = true;
    }
  }


  @Override
  public Path persist(final Map<String, SampleNode> profile,
          @Nullable final String tag,
          final Instant profileFrom, final Instant profileTo) throws IOException {
    if (this.explicitFileName) {
       return save(profile, baseFileName);
    }
    if (tag != null) {
      CharSequences.validatedFileName(tag);
    }
    String timestampedFileName = this.baseFileName + ((tag == null) ? "" : '_' + tag) + '_'
            + DateTimeFormats.COMPACT_TS_FORMAT.format(profileFrom)
            + '_' + DateTimeFormats.COMPACT_TS_FORMAT.format(profileTo);
    timestampedFileName = URLEncoder.encode(timestampedFileName, StandardCharsets.UTF_8.name());
    return save(profile, timestampedFileName);
  }

  @Override
  public Path getTargetPath() {
    return this.targetFolder.toPath();
  }

  @Override
  public String getBaseFileNAme() {
    return this.baseFileName;
  }

  @Override
  public void close() {
    // Nothing to close.
  }


  private static class DumpType {

    private final ProfileFileFormat format;

    private final boolean isGzipped;

    DumpType(final ProfileFileFormat format, final boolean isGzipped) {
      this.format = format;
      this.isGzipped = isGzipped;
    }

    public ProfileFileFormat getFormat() {
      return format;
    }

    public boolean isIsGzipped() {
      return isGzipped;
    }

  }

  @Nullable
  private static DumpType getRequestedFileType(final String pFileName) {
    String fileName = pFileName;
    boolean isGZipped = false;
    if (fileName.endsWith(GZIP_SUFFIX)) {
      fileName = fileName.substring(0, fileName.length() - GZIP_SUFFIX.length());
      isGZipped = true;
    }
    if (fileName.endsWith(ProfileFileFormat.SSDUMP_2.getSuffix())) {
      return new DumpType(ProfileFileFormat.SSDUMP_2, isGZipped);
    } else if (fileName.endsWith(ProfileFileFormat.SSDUMP_3.getSuffix())) {
      return new DumpType(ProfileFileFormat.SSDUMP_3, isGZipped);
    } else {
      return null;
    }
  }

  @Nullable
  private Path save(final Map<String, SampleNode> profile, final String fileName) throws IOException {
    int size = profile.size();
    if (size < 1) {
      return null;
    }
    String newFileName;
    ProfileFileFormat aFormat;
    if (this.explicitFileName) {
      newFileName = fileName;
      aFormat = this.format;
    } else {
      if (size == 1) {
        aFormat = ProfileFileFormat.SSDUMP_2;
        newFileName = Converter.createLabeledSsdump2FileName(fileName,
                  profile.entrySet().iterator().next().getKey());
      } else {
        aFormat = ProfileFileFormat.SSDUMP_3;
        newFileName = fileName + ProfileFileFormat.SSDUMP_3.getSuffix();
      }
      if (this.compress) {
        newFileName += GZIP_SUFFIX;
      }
    }
    return save(newFileName, aFormat, profile);
  }

  @SuppressFBWarnings("UAC_UNNECESSARY_API_CONVERSION_FILE_TO_PATH")
  private Path save(final String fileName, final ProfileFileFormat pFormat, final Map<String, SampleNode> profile)
          throws IOException {
    File file = new File(targetFolder, fileName);
    switch (pFormat) {
      case SSDUMP_2:
        Converter.save(file, profile.entrySet().iterator().next().getValue());
        break;
      case SSDUMP_3:
        Converter.saveLabeledDumps(file, profile);
        break;
      default:
        throw new UnsupportedOperationException("Unsupported file format: " + pFormat);
    }
    return file.toPath();
  }

  @Override
  public ProfilePersister withBaseFileName(final Path ptargetPath, final String pbaseFileName) {
    return new LegacyProfilePersister(ptargetPath, pbaseFileName, this.compress);
  }


  @Override
  public boolean isCompressing() {
    return compress;
  }

  @Override
  public ProfilePersister witCompression(final boolean pcompress) {
    return new LegacyProfilePersister(this.targetFolder, this.baseFileName, pcompress);
  }

  @Override
  public String toString() {
    return "LegacyProfilePersister{" + "baseFileName=" + baseFileName
            + ", targetFolder=" + targetFolder + ", compress=" + compress + '}';
  }


}
