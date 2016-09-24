/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public final class DeletingVisitor implements FileVisitor<Path> {

  private Path rootFolder;

  private PathsIOException exception;

  public DeletingVisitor() {
    rootFolder = null;
    exception = null;
  }

  @Override
  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
    try {
      Files.delete(file);
    } catch (IOException ex) {
      if (rootFolder == null) {
        throw new PathsIOException(file, ex);
      } else {
        suppress(ex, file);
      }
    }
    return FileVisitResult.CONTINUE;
  }

  private void suppress(final IOException ex, final Path path) {
    if (exception == null) {
      exception = new PathsIOException(path, ex);
    } else {
      exception.add(path, ex);
    }
  }

  @Override
  public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
    try {
      Files.delete(dir);
    } catch (IOException ex) {
      suppress(ex, dir);
    }
    if (exception != null && dir.equals(rootFolder)) {
      throw exception;
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
    if (rootFolder == null) {
      rootFolder = dir;
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
    if (rootFolder == null) {
      throw new PathsIOException(file, exc);
    } else {
      suppress(exc, file);
      return FileVisitResult.CONTINUE;
    }
  }

  @Override
  public String toString() {
    return "DeletingVisitor{" + "rootFolder=" + rootFolder + ", exception=" + exception + '}';
  }

}
