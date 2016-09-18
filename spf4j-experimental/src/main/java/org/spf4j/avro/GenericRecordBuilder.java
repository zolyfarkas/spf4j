/*
 * Copyright (c) 2001 - 2016, Zoltan Farkas All Rights Reserved.
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

package org.spf4j.avro;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;
import org.apache.avro.specific.SpecificRecordBase;
import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.CompilerFactoryFactory;

/**
 *
 * @author zoly
 */
public final class GenericRecordBuilder implements Closeable {

  private final File tmpDir;

  private final GenericData.StringType stringType;

  private final AbstractJavaSourceClassLoader source;

  public GenericRecordBuilder() {
    this(GenericData.StringType.String);
  }

  public GenericRecordBuilder(GenericData.StringType stringType) {
    tmpDir = com.google.common.io.Files.createTempDir();
    this.stringType = stringType;
    try {
      AbstractJavaSourceClassLoader src = CompilerFactoryFactory.getDefaultCompilerFactory()
              .newJavaSourceClassLoader(Thread.currentThread().getContextClassLoader());
      src.setSourcePath(new File [] { tmpDir });
      this.source = src;
    } catch (Exception ex) {
     throw new RuntimeException(ex);
    }
  }


  public GenericRecordBuilder addSchema(final Schema schema) {
    SpecificCompiler sc = new SpecificCompiler(schema);
    sc.setStringType(stringType);
    sc.setTemplateDir("org/spf4j/beans/"); // use a custom template that does not contain the builder.
    try {
      sc.compileToDestination(null, tmpDir);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return this;
  }


  public static String getJavaName(final Schema schema) {
    String namespace = schema.getNamespace();
    if (namespace.isEmpty()) {
      return SpecificCompiler.mangle(schema.getName());
    } else {
      return namespace + '.' + SpecificCompiler.mangle(schema.getName());
    }
  }

  public static boolean hasJavaClass(final Schema schema) {
    Schema.Type type = schema.getType();
    switch (type) {
      case ENUM:
      case RECORD:
      case FIXED:
        return true;
      default:
        return false;
    }
  }


  public Class<? extends SpecificRecordBase> getClass(final Schema schema) {
    Preconditions.checkArgument(hasJavaClass(schema), "schema %s has no java class", schema);
    try {
      return (Class<? extends SpecificRecordBase>) source.loadClass(getJavaName(schema));
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void close() {
    try {
      Files.walkFileTree(tmpDir.toPath(), new FileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.deleteIfExists(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.deleteIfExists(dir);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
          throw exc;
        }

      });
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
