package org.spf4j.configDiscovery.maven.plugin;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedInputStream;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Set;
import com.google.common.base.Supplier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.spf4j.base.asm.Invocation;
import org.spf4j.base.asm.Scanner;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true)
public class ConfigScannerMojo
        extends AbstractMojo {

  /**
   * Location of the file.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/avdl",
          property = "outputDir", required = true)
  private File outputDirectory;

  @Parameter(defaultValue = "${project.artifactId}.avdl",
          property = "outputFile", required = true)
  private String fileName;

  @Parameter(defaultValue = "${project.build.sourceEncoding}")
  private String encoding;

  @Parameter(defaultValue = "SystemProperties")
  private String rootRecordName;

  @Parameter(defaultValue = "Config")
  private String recordSuffix;

  @Parameter(defaultValue = "${project.build.directory}/classes")
  private File classes;




  /**
   * target namespace of the configurations.
   */
  @Parameter
  private String namespace;

  private final Set<Method> methods = getSystemPropertyMethods();

  private static Set<Method> getSystemPropertyMethods() {
    try {
      return ImmutableSet.of(System.class.getDeclaredMethod("getProperty", String.class),
              System.class.getDeclaredMethod("getProperty", String.class, String.class),
              Integer.class.getDeclaredMethod("getInteger", String.class),
              Integer.class.getDeclaredMethod("getInteger", String.class, int.class),
              Integer.class.getDeclaredMethod("getInteger", String.class, Integer.class),
              Long.class.getDeclaredMethod("getLong", String.class),
              Long.class.getDeclaredMethod("getLong", String.class, Long.class),
              Long.class.getDeclaredMethod("getLong", String.class, long.class),
              Boolean.class.getDeclaredMethod("getBoolean", String.class));
    } catch (NoSuchMethodException | SecurityException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String greatestCommonPrefix(final String a, final String b) {
    int minLength = Math.min(a.length(), b.length());
    for (int i = 0; i < minLength; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        return a.substring(0, i);
      }
    }
    return a.substring(0, minLength);
  }

  @Nonnull
  public static String getPackageName(final String className) {
    int lastIndexOf = className.lastIndexOf('/');
    if (lastIndexOf >= 0) {
      return className.substring(0, lastIndexOf).replace('/', '.');
    } else {
      return "";
    }
  }


  public void processClasses(final File location, final Map<String, Object> avdlWriter) throws IOException {
    if (!location.exists()) {
      return;
    }
    if (location.isDirectory()) {
      File[] listFiles = location.listFiles();
      for (File file : listFiles) {
        processClasses(file, avdlWriter);
      }
    } else if (location.getName().endsWith(".class")) {
      getLog().debug("Processing class " + location);
      List<Invocation> invocations = Scanner.findUsages(new Supplier<InputStream>() {

        @Override
        public InputStream get() {
          try {
            return new BufferedInputStream(new FileInputStream(location));
          } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
          }
        }
      }, methods);
      for (Invocation invocation : invocations) {
        getLog().debug("Found invocation " + invocation);
        Class<?> returnType = invocation.getInvokedMethod().getReturnType();
        Object[] parameters = invocation.getParameters();
        String caleeClassName = invocation.getCaleeClassName();
        String doc = caleeClassName
                + '.' + invocation.getCaleeMethodName() + ':' + invocation.getCaleeLine();
        Object parameter = parameters[0];
        Map<String, Object> objs = avdlWriter;
        if (parameter instanceof String) {
          String[] attrPath = ((String) parameter).split("\\.");
          for (int i = 0; i < attrPath.length - 1; i++) {
            final String pv = attrPath[i];
            Map<String, Object> get = (Map<String, Object>) objs.get(pv);
            if (get == null) {
              get = new HashMap<>();
              objs.put(pv, get);
            }
            objs = get;
          }
          String fname = attrPath[attrPath.length - 1];
          FieldInfo fi = (FieldInfo) objs.get(fname);
          if (fi == null) {
            if (parameters.length > 1) {
              fi = new FieldInfo(getPackageName(caleeClassName), doc, returnType, parameters[1]);
            } else {
              fi = new FieldInfo(getPackageName(caleeClassName), doc, returnType, null);
            }
            objs.put(fname, fi);
          }
        } else {
          FieldInfo df = (FieldInfo) objs.get("dynamic");
          if (df == null) {
            df = new FieldInfo(getPackageName(caleeClassName), doc, Map.class, Collections.EMPTY_MAP);
          } else {
            df = new FieldInfo(getPackageName(caleeClassName), df.getDoc() + '\n' + doc,
                    Map.class, Collections.EMPTY_MAP);
          }
          objs.put("dynamic", df);
        }

      }
    }
  }

  public static final Map<Class, String> JAVA2AVROTYPE = new HashMap<>();

  static {
    JAVA2AVROTYPE.put(String.class, "string");
    JAVA2AVROTYPE.put(Integer.class, "int");
    JAVA2AVROTYPE.put(int.class, "int");
    JAVA2AVROTYPE.put(Long.class, "long");
    JAVA2AVROTYPE.put(long.class, "long");
    JAVA2AVROTYPE.put(Boolean.class, "boolean");
    JAVA2AVROTYPE.put(boolean.class, "boolean");
    JAVA2AVROTYPE.put(Float.class, "float");
    JAVA2AVROTYPE.put(float.class, "float");
    JAVA2AVROTYPE.put(Double.class, "double");
    JAVA2AVROTYPE.put(double.class, "double");
    JAVA2AVROTYPE.put(Map.class, "map<string>");
  }

  public String writeRecord(final Writer w, final String recordName, final Map<String, Object> record)
          throws IOException {
    // do subRecords first.
    String nameSpace = null;
    Map<String, String> fieldName2Ns = new HashMap<>();
    for (Map.Entry<String, Object> entry : record.entrySet()) {
      Object value = entry.getValue();
      String ns;
      if (value instanceof Map) {
        String key = entry.getKey();
        ns = writeRecord(w, CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, key) + recordSuffix,
                (Map<String, Object>) value);
        fieldName2Ns.put(key, ns);
      } else if (value instanceof FieldInfo) {
        ns = ((FieldInfo) value).getNamespace();
      } else {
        throw new IllegalStateException("Not supported type " + value);
      }
      if (nameSpace == null) {
        nameSpace = ns;
      } else {
        nameSpace = greatestCommonPrefix(nameSpace, ns);
      }
    }
    if (nameSpace.endsWith(".")) {
      nameSpace = nameSpace.substring(0, nameSpace.length() - 1);
    }
    // write record
    w.write(" @namespace(\"");
    w.write(nameSpace);
    w.write("\")\n");
    w.write(" record ");
    w.write(recordName);
    w.write(" {\n");
    for (Map.Entry<String, Object> entry : record.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof FieldInfo) {
        FieldInfo field = (FieldInfo) value;
        w.write("\n  /**");
        w.write(field.getDoc());
        w.write("*/\n");
        Class type = field.getType();
        Object defaultValue = field.getDefaultValue();
        String avroType = JAVA2AVROTYPE.get(type);
        if (avroType == null) {
          throw new IllegalStateException(" No avro equivalent for " + type);
        }
        if (defaultValue == null) {
          w.write("  union {null, ");
          w.write(avroType);
          w.write("} ");
        } else {
          w.write("  ");
          w.write(avroType);
          w.write(" ");
        }
        w.write(entry.getKey());
        w.write(" = ");
        w.append(defaultValue == null ? null :
                defaultValue.getClass() == String.class ? JsonUtils.toJsonString((String) defaultValue)
                        : defaultValue.toString());
        w.write(";\n");

      } else if (value instanceof Map) {
        String key = entry.getKey();
        String ns = fieldName2Ns.get(key);
        w.write("\n /** Category: ");
        w.write(key);
        w.write(" */\n  ");
        w.write(ns);
        w.write('.');
        w.write(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, key));
        w.write(recordSuffix);
        w.write(" ");
        w.write(key);
        w.write(";\n");
      } else {
        throw new IllegalStateException("Not supported type " + value);
      }
    }
    w.write(" }\n\n");
    return nameSpace;
  }

  @Override
  public void execute() throws MojoExecutionException {
    File f = outputDirectory;

    if (!f.exists()) {
      if (!f.mkdirs()) {
        throw new MojoExecutionException("Unable to create directory " + outputDirectory);
      }
    }

    File outFile = new File(f, fileName);
    getLog().info("Creating avdl file at " + outFile);
    try (Writer w = new OutputStreamWriter(new FileOutputStream(outFile), encoding)) {
      if (namespace != null) {
        w.write("@namespace(\"");
        w.write(namespace);
        w.write("\")\n");
      }
      w.write("protocol  ");
      w.write(rootRecordName);
      w.write("Protocol");
      w.write(" {\n");
      Map<String, Object> record = new HashMap<>();
      processClasses(classes, record);
      writeRecord(w, rootRecordName, record);
      w.write("}\n");
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot generate config description", ex);
    }
  }
}
