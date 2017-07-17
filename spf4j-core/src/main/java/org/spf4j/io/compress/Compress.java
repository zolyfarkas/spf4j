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
package org.spf4j.io.compress;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.io.BufferedInputStream;
import org.spf4j.io.Streams;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * @author zoly
 */
@Beta
@SuppressFBWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
@ParametersAreNonnullByDefault
public final class Compress {

  private Compress() {
  }

  @Nonnull
  public static Path zip(final Path fileToCompress) throws IOException {
    Path parent = fileToCompress.getParent();
    if (parent == null) {
      throw new IllegalArgumentException("Not a file: " + fileToCompress);
    }
    Path destFile = parent.resolve(fileToCompress.getFileName() + ".zip");
    return zip(fileToCompress, destFile);
  }

  @Nonnull
  public static Path zip(final Path fileToCompress, final Path destFile) throws IOException {
    Path fNamePath = fileToCompress.getFileName();
    if (fNamePath == null) {
      throw new IllegalArgumentException("Not a file: " + fileToCompress);
    }
    String fileName = fNamePath.toString();
    Path parent = fileToCompress.getParent();
    if (parent == null) {
      throw new IllegalArgumentException("Not a file: " + fileToCompress);
    }
    Path tmpFile = Files.createTempFile(parent, ".", null);
    try {
      try (BufferedOutputStream fos = new BufferedOutputStream(Files.newOutputStream(tmpFile));
              ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
        ZipEntry ze = new ZipEntry(fileName);
        zos.putNextEntry(ze);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(fileToCompress),
                8192, ArraySuppliers.Bytes.TL_SUPPLIER)) {
          Streams.copy(in, zos);
        }
      }
      Files.move(tmpFile, destFile,
              StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(tmpFile);
    }
    return destFile;
  }

  public static void copyFileAtomic(final Path source, final Path destinationFile) throws IOException {
    Path parent = destinationFile.getParent();
    if (parent == null) {
      throw new IllegalArgumentException("Destination " + destinationFile + " is not a file");
    }
    Path tmpFile = Files.createTempFile(parent, ".", null);
    try {
      try (InputStream in = new BufferedInputStream(Files.newInputStream(source),
              8192, ArraySuppliers.Bytes.TL_SUPPLIER);
              OutputStream os = new BufferedOutputStream(Files.newOutputStream(tmpFile))) {
        Streams.copy(in, os);
      }
      Files.move(tmpFile, destinationFile,
              StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(tmpFile);
    }
  }

  @Nonnull
  public static List<Path> unzip(final Path zipFile) throws IOException {
    Path parent = zipFile.getParent();
    if (parent == null) {
      throw new IllegalArgumentException("File " + zipFile + " is not a zip file");
    }
    return unzip(zipFile, parent);
  }

  /**
   * Unzip a zip file to a destination folder.
   * @param zipFile
   * @param destinationDirectory
   * @return the list of files that were extracted.
   * @throws IOException in case extraction fails for whatever reason.
   */
  @Nonnull
  public static List<Path> unzip(final Path zipFile, final Path destinationDirectory) throws IOException {
    if (!Files.exists(destinationDirectory)) {
      Files.createDirectories(destinationDirectory);
    }
    if (!Files.isDirectory(destinationDirectory)) {
      throw new IllegalArgumentException("Destination " + destinationDirectory + " must be a directory");
    }
    final List<Path> response = new ArrayList<>();
    FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + zipFile.toUri().toURL()), Collections.emptyMap());
    try {
      for (Path root : zipFs.getRootDirectories()) {
        Path dest =  Paths.get(destinationDirectory.toString(), root.toString());
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                  throws IOException {
            Path destination = dest.resolve(root.relativize(file).toString());
            copyFileAtomic(file, destination);
            response.add(destination);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                  throws IOException {
            Path np = dest.resolve(dir.toString());
            Files.createDirectories(np);
            return FileVisitResult.CONTINUE;
          }
        });
      }
    } catch (IOException | RuntimeException ex) {
      for (Path path : response) {
        try {
          Files.delete(path);
        } catch (IOException | RuntimeException ex2) {
          ex.addSuppressed(ex2);
        }
      }
      throw ex;
    }
    return response;
  }

}
