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

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;
import org.apache.avro.specific.SpecificRecordBase;
import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.io.DeletingVisitor;
import org.spf4j.io.SetFilesReadOnlyVisitor;

/**
 * Bytecode generating GenericRecord builder.
 *
 * Microbenchmarks show a 10% performance improvement. but depending on your data your performance benefits can
 * be greater or smaller. The main benefit for this is the potential for eliminating the need for separate handling
 * or generic records vs specific records in the avro lib...
 *
 * Benchmark                                                       Mode  Cnt         Score        Error  Units
 * GenericRecordBenchmark.testAvroGenericRecordNew                thrpt   10  36162498.300 ± 246967.965  ops/s
 * GenericRecordBenchmark.testSpf4jGenericRecordNew               thrpt   10  39773579.353 ± 939582.354  ops/s
 * GenericRecordBenchmark.testAvroGenericRecordNewSetGet          thrpt   10  36274035.575 ± 476132.764  ops/s
 * GenericRecordBenchmark.testSpf4jGenericRecordNewSetGet         thrpt   10  40111265.012 ± 600216.931  ops/s
 *
 *
 *
 * @author zoly
 */
@Beta
public final class GenericRecordBuilder implements Closeable {

  private final File tmpDir;

  private final GenericData.StringType stringType;

  private final AbstractJavaSourceClassLoader source;

  public GenericRecordBuilder(final Schema ... schemas) {
    this(GenericData.StringType.String, schemas);
  }

  public GenericRecordBuilder(final GenericData.StringType stringType, final Schema ... schemas) {
    tmpDir = com.google.common.io.Files.createTempDir();
    this.stringType = stringType;
    generateClasses(stringType, schemas);
    try {
      AbstractJavaSourceClassLoader src = CompilerFactoryFactory.getDefaultCompilerFactory()
              .newJavaSourceClassLoader(Thread.currentThread().getContextClassLoader());
      src.setSourcePath(new File[] {tmpDir});
      this.source = src;
    } catch (Exception ex) {
     throw new RuntimeException(ex);
    }
  }


  private void generateClasses(final GenericData.StringType st, final Schema ...schemas) {
    String[] namespaces = new String[schemas.length];
    for (int i = 0; i < schemas.length; i++) {
      String namespace = schemas[i].getNamespace();
      if (namespace == null) {
        namespace = "";
      }
      namespaces[i] = namespace;
    }
    String commonPrefix = org.spf4j.base.Strings.commonPrefix(namespaces);
    if (commonPrefix.endsWith(".")) {
      commonPrefix = commonPrefix.substring(0, commonPrefix.length() - 1);
    }
    Protocol proto = new Protocol("generated", commonPrefix);
    proto.setTypes(Arrays.asList(schemas));
    SpecificCompiler sc = new SpecificCompiler(proto);
    sc.setStringType(st);
     // use a custom template that does not contain the builder (janino can't compile builder).
    sc.setTemplateDir("org/spf4j/avro/");
    try {
      sc.compileToDestination(null, tmpDir);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    try {
      Files.walkFileTree(tmpDir.toPath(), new SetFilesReadOnlyVisitor());
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }


  public Class<? extends SpecificRecordBase> getClass(final Schema schema) {
    Preconditions.checkArgument(Schemas.hasGeneratedJavaClass(schema), "schema %s has no java class", schema);
    try {
      return (Class<? extends SpecificRecordBase>) source.loadClass(Schemas.getJavaClassName(schema));
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void close() {
    try {
      Files.walkFileTree(tmpDir.toPath(), new DeletingVisitor());
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public String toString() {
    return "GenericRecordBuilder{" + "tmpDir=" + tmpDir + ", stringType=" + stringType + ", source=" + source + '}';
  }




}
