package org.spf4j.beans;

import com.google.common.io.Files;
import java.io.File;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;
import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.CompilerFactoryFactory;

/**
 *
 * @author zoly
 */
public final class GenericRecordBuilder {

  private GenericRecordBuilder() { }

  public static Class<? extends GenericRecord> createClass(final Schema schema) throws Exception {
    SpecificCompiler sc = new SpecificCompiler(schema);
    sc.setStringType(GenericData.StringType.String);
    sc.setTemplateDir("org/spf4j/beans/");
    File tmp = Files.createTempDir();
    sc.compileToDestination(null, tmp);
    AbstractJavaSourceClassLoader source
            = CompilerFactoryFactory.getDefaultCompilerFactory()
                    .newJavaSourceClassLoader(Thread.currentThread().getContextClassLoader());
    source.setSourcePath(new File[]{tmp});
    return (Class<? extends GenericRecord>) source.loadClass(schema.getFullName());
  }
}
