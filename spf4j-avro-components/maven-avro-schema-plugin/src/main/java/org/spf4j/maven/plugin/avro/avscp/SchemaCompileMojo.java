package org.spf4j.maven.plugin.avro.avscp;

import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
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
import java.util.stream.Stream;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.SchemaRefWriter;
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
 * Mojo that will compile the avro sources: *.avsc, *.avpr, *.avdl in: 1) java files. 2) avsc files.
 *
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
   * add maven coordinates to the schema. (group:artifact:version:ID) ID->Schema full name mapping file
   * schema_index.properties is packaged in the jar artifacts.
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


  /**
   * string type
   */
  @Parameter(name = "stringType", defaultValue = "String")
  private String stringType = "String";

  private int idSequence = 0;

  private final Map<String, Schema> index = new HashMap<>();

  private String attachMavenId(final Schema schema) {
    String exMvnId = schema.getProp("mvnId");
    if (exMvnId == null) {
      String newId = genMnvId(schema);
      schema.addProp("mvnId", newId);
      return newId;
    } else {
      return exMvnId;
    }
  }

  public CharSequence getPackageMvnIdPrefix() {
    StringBuilder idBuilder = new StringBuilder(64);
    idBuilder.append(mavenProject.getGroupId()).append(':').append(mavenProject.getArtifactId())
            .append(':').append(mavenProject.getVersion());
    return idBuilder;
  }

  public String genMnvId(final Schema schema) {
    StringBuilder idBuilder = new StringBuilder(64);
    idBuilder.append(mavenProject.getGroupId()).append(':').append(mavenProject.getArtifactId())
            .append(':').append(mavenProject.getVersion());
    StringBuilder idb = new StringBuilder(4);
    AppendableUtils.appendUnsignedString(idb, idSequence, 5);
    idBuilder.append(':').append(idb);
    idSequence++;
    index.put(idb.toString(), schema);
    return idBuilder.toString();
  }

  protected void doCompileIDL(final File sourceDir,  final String filename) throws IOException {
    Thread currentThread = Thread.currentThread();
    ClassLoader contextClassLoader = currentThread.getContextClassLoader();
    try {
      List<URL> runtimeUrls = createPathUrls(sourceDir);
      URLClassLoader projPathLoader = AccessController.doPrivileged(
              (PrivilegedAction<URLClassLoader>) ()
              -> new URLClassLoader(runtimeUrls.toArray(new URL[runtimeUrls.size()]), contextClassLoader));
      currentThread.setContextClassLoader(projPathLoader);
      File file = new File(sourceDir, filename);
      String sourceAbsolutePath = sourceDir.getAbsolutePath();
      // set the current dir do that sourceIdl will be computed relative to it.
      // This makes this plugin not thread safe.
      Idl parser;
      String origCurrentDir = org.spf4j.base.Runtime.getCurrentDir();
      org.spf4j.base.Runtime.setCurrentDir(sourceAbsolutePath);
      try {
        parser = new Idl(file, projPathLoader);
      } finally {
        org.spf4j.base.Runtime.setCurrentDir(origCurrentDir);
      }
      Protocol protocol = parser.CompilationUnit();
      publishSchemasAndAttachMvnIdToProtocol(protocol, false, useSchemaReferencesForAvsc);
      SpecificCompiler compiler = new SpecificCompiler(protocol);
      compiler.setOutputCharacterEncoding(mavenProject.getProperties().getProperty("project.build.sourceEncoding"));
      compiler.setStringType(GenericData.StringType.valueOf(stringType));
      compiler.setTemplateDir(templateDirectory);
      compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(fieldVisibility));
      compiler.setCreateSetters(createSetters);
      compiler.compileToDestination(null, generatedJavaTarget);
    } catch (ParseException e) {
      throw new IOException(e);
    } catch (DependencyResolutionRequiredException drre) {
      throw new IOException(drre);
    } finally {
      currentThread.setContextClassLoader(contextClassLoader);
    }
  }

  public List<URL> createPathUrls(final File sourceFolder)
          throws MalformedURLException, DependencyResolutionRequiredException {
    List<String> cpElements = mavenProject.getRuntimeClasspathElements();
    List<URL> runtimeUrls = new ArrayList<URL>();
    runtimeUrls.add(sourceFolder.toURI().toURL());
    // If runtimeClasspathElements is not empty values add its values to Idl path.
    if (cpElements != null && !cpElements.isEmpty()) {
      for (Object runtimeClasspathElement : cpElements) {
        String element = (String) runtimeClasspathElement;
        runtimeUrls.add(new File(element).toURI().toURL());
      }
    }
    return runtimeUrls;
  }

  private File addMvnIdsToIdl(final File idl, final URLClassLoader cl)
          throws IOException, ParseException {
    if (!addMavenId) {
      return idl;
    }
    String charsetStr = mavenProject.getProperties().getProperty("project.build.sourceEncoding");
    Charset charset = charsetStr == null ? Charset.defaultCharset() : Charset.forName(charsetStr);
    List<String> readAllLines = Files.readAllLines(idl.toPath(), charset);
    Idl parser = new Idl(idl, cl);
    Protocol protocol = parser.CompilationUnit();
    for (Schema s : protocol.getTypes()) {
      if (s.getProp("mvnId") != null) {
        continue;
      }
      String sourceIdl = s.getProp("sourceIdl");
      if (sourceIdl == null) {
        getLog().warn("sourceIdl not available, will not attach mvnId for IDLs");
        continue;
      }
      int cidxS = sourceIdl.lastIndexOf(':');
      int colIndex = Integer.parseInt(sourceIdl.substring(cidxS + 1)) - 1;
      int ridxS = sourceIdl.lastIndexOf(':', cidxS - 1);
      int rowIndex = Integer.parseInt(sourceIdl.substring(ridxS + 1, cidxS)) - 1;
      File location = new File(sourceIdl.substring(0, ridxS));
      if (!location.getName().equals(idl.getName())) {
        continue;
      }
      String line = readAllLines.get(rowIndex);
      getLog().debug("inserting mvnId at "
              + rowIndex + ':' + colIndex + " for line \"" + line + "\" schema: " + s);
      readAllLines.set(rowIndex, line.substring(0, colIndex)
              + " @mvnId(\"" + genMnvId(s) + "\") "
              + line.substring(colIndex, line.length()));
    }
    Path tempIdl = Files.createTempFile(this.target.toPath(), idl.getName(), ".tmp");
    Files.write(tempIdl, readAllLines, charset);
    return tempIdl.toFile();
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
              StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      SpecificCompiler compiler = new SpecificCompiler(schema);
      compiler.setOutputCharacterEncoding(mavenProject.getProperties().getProperty("project.build.sourceEncoding"));
      compiler.setTemplateDir(templateDirectory);
      compiler.setStringType(GenericData.StringType.valueOf(stringType));
      compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(fieldVisibility));
      compiler.setCreateSetters(createSetters);
      compiler.compileToDestination(src, generatedJavaTarget);
    }
  }

  protected void doCompileProtocol(final String filename, final Path destination) throws IOException {
    File src = new File(sourceDirectory, filename);
    Protocol protocol = Protocol.parse(src);
    publishSchemasAndAttachMvnIdToProtocol(protocol, addMavenId, useSchemaReferencesForAvsc);
    SpecificCompiler compiler = new SpecificCompiler(protocol);
    compiler.setOutputCharacterEncoding(mavenProject.getProperties().getProperty("project.build.sourceEncoding"));
    compiler.setTemplateDir(templateDirectory);
    compiler.setStringType(GenericData.StringType.valueOf(stringType));
    compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.valueOf(fieldVisibility));
    compiler.setCreateSetters(createSetters);
    compiler.compileToDestination(src, generatedJavaTarget);
    Files.write(destination,
            protocol.toString(true).getBytes(StandardCharsets.UTF_8));
  }



  private void publishSchemasAndAttachMvnIdToProtocol(final Protocol protocol,
          final boolean addMvnId, final boolean useSchemaReferences) throws IOException {
    Collection<Schema> types = protocol.getTypes();
    Set<String> typeNames = Sets.newHashSetWithExpectedSize(types.size());
    for (Schema schema : types) {
      String fullName = schema.getFullName();
      if (!typeNames.add(fullName)) {
        continue;
      }
      String targetName = fullName.replace('.', File.separatorChar) + ".avsc";
      Path destinationFile = generatedAvscTarget.toPath().resolve(targetName);
      Path parent = destinationFile.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      if (addMvnId) {
        attachMavenId(schema);
      }
      if (useSchemaReferences) {
        try (OutputStream fos =
                new BufferedOutputStream(Files.newOutputStream(destinationFile,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
          SchemaRefWriter.write(schema, fos);
        }
      } else {
        Files.write(destinationFile, schema.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      }
    }
  }

  public void deleteGeneratedAvailableInDependencies() throws IOException {
    Path classesInfo = dependenciesDirectory.toPath().resolve("classes.txt");
    Set<String> classes = new HashSet(Files.readAllLines(classesInfo, StandardCharsets.UTF_8));
    Path javaPath = generatedJavaTarget.toPath();
    try (Stream<Path> fsStream = Files.walk(javaPath)) {
      List<Path> dupes = fsStream
              .filter((p) -> {
                Path relativize = javaPath.relativize(p);
                return classes.contains(relativize.toString().replace(".java", ".class"));
              }).collect(Collectors.toList());
      for (Path p : dupes) {
        Files.delete(p);
        getLog().info("Deleted dupes: " + dupes);
      }
    }
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
    try (Stream<Path> fsStream = Files.walk(javaPath)) {
      List<Path> protocolFiles = fsStream
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
    super.execute();
    Log logger = this.getLog();
    logger.info("Generationg java code + schemas, using avro "
            + PackageInfo.getPackageInfo(org.apache.avro.Schema.class.getName()));
    synchronized (String.class) {
      Properties properties = new Properties(System.getProperties());
      try {
        for (Map.Entry<String, String> entry : ((Set<Map.Entry<String, String>>) ((Set) systemProperties.entrySet()))) {
          System.setProperty(entry.getKey(), entry.getValue());
        }
        Path generatedAvscTargetPath = generatedAvscTarget.toPath();
        Files.createDirectories(generatedAvscTargetPath);
        Files.createDirectories(generatedJavaTarget.toPath());
        String[] sourceFiles = getSourceFiles("**/*.avsc");
        try {
          doCompileSchemas(sourceFiles);
        } catch (IOException ex) {
          throw new MojoExecutionException("cannot compile schemas " + Arrays.toString(sourceFiles), ex);
        }
        Path tmpSourceTarget = this.target.toPath().resolve("avro-sources");
        compileAvpr(tmpSourceTarget);
        addMvnIdToIdlsAndMoveToDestination(tmpSourceTarget);
        compileIdl(tmpSourceTarget);

        Path codegenManifest = generatedAvscTargetPath.resolve(SCHEMA_MANIFEST);
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
        Path indexFile = generatedAvscTargetPath.resolve("schema_index.properties");
        try (BufferedWriter bw = Files.newBufferedWriter(indexFile,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
          bw.append("_pkg=");
          bw.append(getPackageMvnIdPrefix());
          bw.append('\n');
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
        resource2.setDirectory(tmpSourceTarget.toString());
        resource2.addInclude("**/*.avpr");
        resource2.addInclude("**/*.avdl");
        mavenProject.addResource(resource2);
      } catch (IOException ex) {
        throw new MojoExecutionException("cannot compile schemas, cfg = " + this, ex);
      } finally {
        System.setProperties(properties);
      }
    }
  }

  public void compileIdl(final Path pSources) throws MojoExecutionException {
    File pSourcesFile = pSources.toFile();
    for (String file : getFiles(pSourcesFile, "**/*.avdl")) {
      try {
        doCompileIDL(pSourcesFile, file);
      } catch (IOException ex) {
        throw new MojoExecutionException("cannot compile " + file, ex);
      }
    }
  }

  public void addMvnIdToIdlsAndMoveToDestination(final Path destPath) throws MojoExecutionException {
    Thread currentThread = Thread.currentThread();
    ClassLoader contextClassLoader = currentThread.getContextClassLoader();
    try {
      List<URL> runtimeUrls = createPathUrls(this.sourceDirectory);
      getLog().info("Compile classpath: " + runtimeUrls);
      URLClassLoader projPathLoader = AccessController.doPrivileged(
              (PrivilegedAction<URLClassLoader>) ()
                      -> new URLClassLoader(runtimeUrls.toArray(new URL[runtimeUrls.size()]), contextClassLoader));
      currentThread.setContextClassLoader(projPathLoader);
      for (String file : getSourceFiles("**/*.avdl")) {
        Path destination = destPath.resolve(file);
        Path parent = destination.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        File idlFile = new File(sourceDirectory, file);
        try {
          idlFile = addMvnIdsToIdl(idlFile, projPathLoader);
        } catch (ParseException | IOException | RuntimeException ex) {
          throw new MojoExecutionException("cannot add mvnId to  IDL " + idlFile + ", " + ex.getMessage(), ex);
        }
        Files.copy(idlFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException | DependencyResolutionRequiredException ex) {
      throw new MojoExecutionException("cannot add mvnId to  IDL " + this, ex);
    } finally {
      currentThread.setContextClassLoader(contextClassLoader);
    }
  }

  public void compileAvpr(final Path pSources) throws MojoExecutionException {
    for (String file : getSourceFiles("**/*.avpr")) {
      try {
        Path destination = pSources.resolve(file);
        Path folder = destination.getParent();
        if (folder != null) {
          Files.createDirectories(folder);
        }
        doCompileProtocol(file, destination);
      } catch (IOException ex) {
        throw new MojoExecutionException("cannot compile protocol " + file, ex);
      }
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

  public static String[] getFiles(final File directory, final String pattern) {
    FileSetManager fsm = new FileSetManager();
    FileSet fs = new FileSet();
    fs.setDirectory(directory.getAbsolutePath());
    fs.addInclude(pattern);
    fs.setFollowSymlinks(false);
    return fsm.getIncludedFiles(fs);
  }

  @Override
  public String toString() {
    return "SchemaCompileMojo{" + "fieldVisibility=" + fieldVisibility
            + ", templateDirectory=" + templateDirectory + ", createSetters=" + createSetters
            + ", addMavenId=" + addMavenId + ", " + super.toString() + '}';
  }

}
