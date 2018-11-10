package org.spf4j.maven.plugin.avro.avscp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.spf4j.base.AppendableUtils;
import org.spf4j.base.PackageInfo;

/**
 * Mojo that will compile the avro sources: *.avsc, *.avpr, *.avdl in:
 * 1) java files.
 * 2) avsc files.
 * @author Zoltan Farkas
 */
@Mojo(name = "avro-compile",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public final class SchemaCompileMojo
        extends SchemaMojoBase {

   public static final String SCHEMA_MANIFEST = "codegen.properties";

  /**
   * The field visibility indicator for the fields of the generated class, as string values of
   * SpecificCompiler.FieldVisibility. The text is case insensitive.
   */
  @Parameter(name = "fieldVisibility", defaultValue = "PRIVATE")
  private String fieldVisibility;

  /**
   * The directory (within the java classpath) that contains the velocity templates to use for code generation. The
   * default value points to the templates included with the avro-maven-plugin.
   */
  @Parameter(name = "templateDirectory",
          defaultValue = "/org/apache/avro/compiler/specific/templates/java/classic/")
  private String templateDirectory;

  /**
   * Determines whether or not to create setters for the fields of the record. The default is to create setters.
   */
  @Parameter(name = "createSetters",
          defaultValue = "false")
  private boolean createSetters;

  /**
   * add maven coordinates to the schema. (group:artifact:version:ID)
   * ID->Schema full name mapping file  schema_index.properties is packaged in the jar artifacts.
   */
  @Parameter(name = "addMavenId",
          defaultValue = "true")
  private boolean addMavenId = true;

  /**
   * delete Protocol java files, this is when only the schema definitions are relevant.
   */
  @Parameter(name = "deleteProtocolInterface",
          defaultValue = "true")
  private boolean deleteProtocolInterface = true;

  /**
   * set Java system properties that might control avro behavior.
   */
  @Parameter(name = "systemProperties")
  private Properties systemProperties = new Properties();

  private int idSequence = 0;

  private final Map<String, Schema> index = new HashMap<>();

  private void attachMavenId(final Schema schema) {
    if (schema.getProp("mvnId") == null) {
      StringBuilder idBuilder = new StringBuilder(64);
      idBuilder.append(mavenProject.getGroupId()).append(':').append(mavenProject.getArtifactId())
              .append(':').append(mavenProject.getVersion());
      StringBuilder idb = new StringBuilder(4);
      AppendableUtils.appendUnsignedString(idb, idSequence, 5);
      idBuilder.append(':').append(idb);
      schema.addProp("mvnId", idBuilder.toString());
      index.put(idb.toString(), schema);
      idSequence++;
    }
  }

  protected void doCompileIDL(final String filename) throws IOException {
    try {
      List<String> cpElements = mavenProject.getCompileClasspathElements();
      Idl parser;

      List<URL> runtimeUrls = new ArrayList<URL>();

      // Add the source directory of avro files to the classpath so that
      // imports can refer to other idl files as classpath resources
      runtimeUrls.add(sourceDirectory.toURI().toURL());
      runtimeUrls.add(dependenciesDirectory.toURI().toURL());

      // If runtimeClasspathElements is not empty values add its values to Idl path.
      if (cpElements != null && !cpElements.isEmpty()) {
        for (Object runtimeClasspathElement : cpElements) {
          String element = (String) runtimeClasspathElement;
          runtimeUrls.add(new File(element).toURI().toURL());
        }
      }
      getLog().info("Compile classpath: " + runtimeUrls);
      URLClassLoader projPathLoader = AccessController.doPrivileged(
              (PrivilegedAction<URLClassLoader>) ()
                      -> new URLClassLoader(runtimeUrls.toArray(new URL[runtimeUrls.size()]),
              Thread.currentThread().getContextClassLoader()));
      File file = new File(sourceDirectory, filename);
      parser = new Idl(file, projPathLoader);
      Protocol protocol = parser.CompilationUnit();
      Collection<Schema> types = protocol.getTypes();
      for (Schema schema : types) {
        if (addMavenId) {
          attachMavenId(schema);
        }
        String targetName = schema.getFullName().replace('.', File.separatorChar) + ".avsc";
        Path destinationFile = generatedAvscTarget.toPath().resolve(targetName);
        Path parent = destinationFile.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        Files.write(destinationFile,
                schema.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE);
      }
      SpecificCompiler compiler = new SpecificCompiler(protocol);
      compiler.setOutputCharacterEncoding(mavenProject.getProperties().getProperty("project.build.sourceEncoding"));
      compiler.setStringType(GenericData.StringType.String);
      compiler.setTemplateDir(templateDirectory);
      compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(fieldVisibility));
      compiler.setCreateSetters(createSetters);
      compiler.compileToDestination(null, generatedJavaTarget);
    } catch (ParseException e) {
      throw new IOException(e);
    } catch (DependencyResolutionRequiredException drre) {
      throw new IOException(drre);
    }
  }

  protected void doCompileSchemas(final String[] filenames)
          throws IOException {
    Schema.Parser parser = new Schema.Parser();
    for (String fileName : filenames) {
      File src = new File(sourceDirectory, fileName);
      Schema schema = parser.parse(src);
      if (addMavenId) {
        attachMavenId(schema);
      }
      String targetName = schema.getFullName().replace('.', File.separatorChar) + ".avsc";
      Path destination = generatedAvscTarget.toPath().resolve(targetName);
      Path parent = destination.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(destination,
              schema.toString().getBytes(StandardCharsets.UTF_8),
              StandardOpenOption.CREATE);
      SpecificCompiler compiler = new SpecificCompiler(schema);
      compiler.setOutputCharacterEncoding(mavenProject.getProperties().getProperty("project.build.sourceEncoding"));
      compiler.setTemplateDir(templateDirectory);
      compiler.setStringType(GenericData.StringType.String);
      compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(fieldVisibility));
      compiler.setCreateSetters(createSetters);
      compiler.compileToDestination(src, generatedJavaTarget);
    }
  }

  protected void doCompileProtocol(final String filename) throws IOException {
    File src = new File(sourceDirectory, filename);
    Protocol protocol = Protocol.parse(src);
    Collection<Schema> types = protocol.getTypes();
    for (Schema schema : types) {
      if (addMavenId) {
        attachMavenId(schema);
      }
      String targetName = schema.getFullName().replace('.', File.separatorChar) + ".avsc";
      Path destinationFile = generatedAvscTarget.toPath().resolve(targetName);
      Path parent = destinationFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(destinationFile, schema.toString().getBytes(StandardCharsets.UTF_8),
              StandardOpenOption.CREATE);
    }
    SpecificCompiler compiler = new SpecificCompiler(protocol);
    compiler.setOutputCharacterEncoding(mavenProject.getProperties().getProperty("project.build.sourceEncoding"));
    compiler.setTemplateDir(templateDirectory);
    compiler.setStringType(GenericData.StringType.String);
    compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(fieldVisibility));
    compiler.setCreateSetters(createSetters);
    compiler.compileToDestination(src, generatedJavaTarget);
  }

  public void deleteGeneratedAvailableInDependencies() throws IOException {
    Path classesInfo = dependenciesDirectory.toPath().resolve("classes.txt");
    Set<String> classes = new HashSet(Files.readAllLines(classesInfo, StandardCharsets.UTF_8));
    Path javaPath = generatedJavaTarget.toPath();
    List<Path> dupes = Files.walk(javaPath)
            .filter((p) -> {
              Path relativize = javaPath.relativize(p);
              return classes.contains(relativize.toString().replace(".java", ".class"));
            }).collect(Collectors.toList());
    for (Path p : dupes) {
      Files.delete(p);
    }
    getLog().info("Deleted dupes: " + dupes);
  }

  public void deleteProtocolClasses() throws IOException {
   String detectionString = "org.apache.avro.Protocol PROTOCOL";
    Path javaPath = generatedJavaTarget.toPath();
    String mSourceEncoding = mavenProject.getProperties().getProperty("project.build.sourceEncoding");
    String sourceEncoding;
    if (mSourceEncoding == null) {
      sourceEncoding = Charset.defaultCharset().name();
    } else {
      sourceEncoding = mSourceEncoding;
    }
    List<Path> protocolFiles = Files.walk(javaPath)
            .filter((p) -> {
              Path fileName = p.getFileName();
              if (fileName == null || !fileName.toString().endsWith(".java")) {
                return false;
              }
              try (BufferedReader br = Files.newBufferedReader(p, Charset.forName(sourceEncoding))) {
                String line;
                while ((line = br.readLine()) != null) {
                  if (line.contains(detectionString)) {
                    return true;
                  }
                }
              } catch (IOException ex) {
                getLog().info("cannot read file " + p + ", ignoring for cleanup", ex);
              }
              return false;
            }).collect(Collectors.toList());
    for (Path p : protocolFiles) {
      Files.delete(p);
    }
  }

  public void deleteSchemasAvailableInDependencies(final Path schTargetPath) throws IOException {
    Path classesInfo = dependenciesDirectory.toPath();
    Set<Path> schemas = Files.walk(classesInfo).filter(
            (p) -> {
              Path fileName = p.getFileName();
              return fileName == null ? false : fileName.toString().endsWith("avsc");
            })
            .map((p) -> classesInfo.relativize(p)).collect(Collectors.toSet());
    List<Path> dupes = Files.walk(schTargetPath).filter((p) -> schemas.contains(schTargetPath.relativize(p)))
            .collect(Collectors.toList());
    for (Path p : dupes) {
      Files.delete(p);
    }
    getLog().info("Deleted dupes: " + dupes);
  }




  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log logger = this.getLog();
    logger.info("Generationg java code + schemas, using avro "
            + PackageInfo.getPackageInfo(org.apache.avro.Schema.class.getName()));
    synchronized (String.class) {
      for (Map.Entry<String, String> entry : ((Set<Map.Entry<String, String>>) ((Set) systemProperties.entrySet()))) {
        System.setProperty(entry.getKey(), entry.getValue());
      }
      Path pSources = this.target.toPath().resolve("avro-sources");
      String[] sourceFiles = getSourceFiles("**/*.avsc");
      try {
        doCompileSchemas(sourceFiles);
      } catch (IOException ex) {
        throw new MojoExecutionException("cannot compile schemas " + Arrays.toString(sourceFiles), ex);
      }

      for (String file : getSourceFiles("**/*.avpr")) {
        try {
          doCompileProtocol(file);
          Path destination = pSources.resolve(file);
          Path folder = destination.getParent();
          if (folder != null) {
            Files.createDirectories(folder);
          }
          Files.copy(sourceDirectory.toPath().resolve(file), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
          throw new MojoExecutionException("cannot compile protocol " + file, ex);
        }
      }
      for (String file : getSourceFiles("**/*.avdl")) {
        try {
          doCompileIDL(file);
          Path destination = pSources.resolve(file);
          Path parent = destination.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          Files.copy(sourceDirectory.toPath().resolve(file), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
          throw new MojoExecutionException("cannot compile IDL " + file, ex);
        }
      }
      Path codegenManifest = generatedAvscTarget.toPath().resolve(SCHEMA_MANIFEST);
      try {
        Files.write(codegenManifest,
             Collections.singletonList("Build-Time=" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + '\n'),
             StandardCharsets.UTF_8);
      } catch (IOException ex) {
        throw new MojoExecutionException("Cannot create codegen manifest file " + codegenManifest, ex);
      }
      try {
        deleteGeneratedAvailableInDependencies();
        deleteSchemasAvailableInDependencies(getGeneratedAvscTarget().toPath());
        if (deleteProtocolInterface) {
          deleteProtocolClasses();
        }
      } catch (IOException ex) {
        throw new MojoExecutionException("Cannot delete dependency dupes " + this, ex);
      }
      Path indexFile = this.generatedAvscTarget.toPath().resolve("schema_index.properties");
      try (BufferedWriter bw = Files.newBufferedWriter(indexFile,
              StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
        for (Map.Entry<String, Schema> entry : index.entrySet()) {
            bw.append(entry.getKey());
            bw.append('=');
            bw.append(entry.getValue().getFullName());
            bw.append('\n');
        }
      } catch (IOException ex) {
        throw new MojoExecutionException("Cannot generate schema index " + this, ex);
      }
      mavenProject.addCompileSourceRoot(generatedJavaTarget.getAbsolutePath());
      Resource resource = new Resource();
      resource.setDirectory(this.generatedAvscTarget.getAbsolutePath());
      resource.addInclude("**/*.avsc");
      resource.addInclude("*.properties");
      mavenProject.addResource(resource);
      Resource resource2 = new Resource();
      resource2.setDirectory(pSources.toString());
      resource2.addInclude("**/*.avpr");
      resource2.addInclude("**/*.avdl");
      mavenProject.addResource(resource2);
    }
  }

  public String[] getSourceFiles(final String pattern) {
    FileSetManager fsm = new FileSetManager();
    FileSet fs = new FileSet();
    fs.setDirectory(sourceDirectory.getAbsolutePath());
    fs.addInclude(pattern);
    fs.setFollowSymlinks(false);
    return fsm.getIncludedFiles(fs);
  }

  @Override
  public String toString() {
    return "SchemaCompileMojo{" + "fieldVisibility=" + fieldVisibility
            + ", templateDirectory=" + templateDirectory + ", createSetters=" + createSetters
            + ", addMavenId=" + addMavenId + '}';
  }

}
