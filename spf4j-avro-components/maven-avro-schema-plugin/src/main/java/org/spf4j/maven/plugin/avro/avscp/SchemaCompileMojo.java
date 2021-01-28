package org.spf4j.maven.plugin.avro.avscp;

import com.google.common.collect.Maps;
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
import org.apache.avro.ExtendedParser;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.SchemaRefWriter;
import org.apache.avro.UnresolvedExtendedParser;
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.Version;
import org.spf4j.base.AppendableUtils;
import org.spf4j.base.PackageInfo;
import org.spf4j.io.compress.Compress;
import org.spf4j.maven.MavenRepositoryUtils;

/**
 * Mojo that will compile the avro sources: *.avsc, *.avpr, *.avdl in: 1) java files. 2) avsc files.
 *
 * @author Zoltan Farkas
 */
@Mojo(name = "avro-compile",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@SuppressFBWarnings({ "PATH_TRAVERSAL_IN", "SE_BAD_FIELD_INNER_CLASS" })
public final class SchemaCompileMojo
        extends SchemaMojoBase {

  public static final String SCHEMA_INDEX_FILENAME = "schema_index.properties";

  public static final String SCHEMA_INDEX_PGK_KEY = "_pkg";

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


  private final Map<String, Integer> prevReleaseName2Index = new HashMap();

  @SuppressWarnings("unchecked")
  private void loadPrevReleaseId2Map() throws IOException {
    MavenProject mavenProject = getMavenProject();
    Log log = getLog();
    String versionRange = "[," + mavenProject.getVersion() +  ')';
    String groupId = mavenProject.getGroupId();
    String artifactId = mavenProject.getArtifactId();
    List<RemoteRepository> remoteProjectRepositories = mavenProject.getRemoteProjectRepositories();
    RepositorySystem repoSystem = getRepoSystem();
    RepositorySystemSession repositorySession = getMavenSession().getRepositorySession();
    List<Version> rangeVersions;
    try {
      rangeVersions = MavenRepositoryUtils.getVersions(groupId, artifactId, versionRange,
              remoteProjectRepositories, repoSystem, repositorySession);
    } catch (VersionRangeResolutionException ex) {
      throw new RuntimeException("Invalid compatibiliy.versionRange = " + versionRange + " setting", ex);
    }
    rangeVersions = rangeVersions.stream().filter((v) -> !v.toString().endsWith("SNAPSHOT"))
            .collect(Collectors.toList());
    int tSize = rangeVersions.size();
    rangeVersions = rangeVersions.subList(Math.max(tSize - 1, 0), tSize);
    log.info("Loading id 2 name map from " + rangeVersions);
    if (rangeVersions.isEmpty()) {
      return;
    }
    Version version  = rangeVersions.get(0);
    Path targetPath = getTarget().toPath();
    File prevSchemaArchive;
    try {
      prevSchemaArchive = MavenRepositoryUtils.resolveArtifact(
              groupId, artifactId, schemaArtifactClassifier, schemaArtifactExtension, version.toString(),
              remoteProjectRepositories, repoSystem, repositorySession);
    } catch (ArtifactResolutionException ex) {
      throw new RuntimeException("Cannot resolve previous version "  + version, ex);
    }
    Path dest = targetPath.resolve("prevSchema").resolve(version.toString());
    Files.createDirectories(dest);
    log.debug("Unzipping " + prevSchemaArchive + " to " + dest);
    List<Path> indexFiles = Compress.unzip2(prevSchemaArchive.toPath(), dest, (Path p) -> {
      Path fileName = p.getFileName();
      if (fileName == null) {
        return false;
      }
      return SCHEMA_INDEX_FILENAME.equals(fileName.toString());
    });
    Properties prevIndex = new Properties();
    if (indexFiles.size() != 1) {
      log.info("no index file or to many in previous version: " + indexFiles);
    } else {
      // load previous index file
      Path indexFile = indexFiles.get(0);
      try (BufferedReader br = Files.newBufferedReader(indexFile, StandardCharsets.UTF_8)) {
        prevIndex.load(br);
      }
    }
    for (Map.Entry<String, String> entry : (Set<Map.Entry<String, String>>) (Set) prevIndex.entrySet()) {
      String key = entry.getKey();
      if (SCHEMA_INDEX_PGK_KEY.equals(key)) {
        continue;
      }
      int idx = Integer.parseInt(key, 32);
      if (idx >= idSequence) {
        idSequence = idx + 1;
      }
      prevReleaseName2Index.put(entry.getValue(), idx);
    }
    log.debug("loaded existing mappings: " + prevReleaseName2Index);
    log.info("loaded existing mappings, new id sequence: " + idSequence);
  }

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
    Integer idx = prevReleaseName2Index.get(schema.getFullName());
    if (idx == null) {
      idx = idSequence++;
    }
    StringBuilder idb = new StringBuilder(4);
    AppendableUtils.appendUnsignedString(idb, idx, 5);
    idBuilder.append(':').append(idb);
    index.put(idb.toString(), schema);
    return idBuilder.toString();
  }

  protected void doCompileIDL(final File sourceDir,  final String filename) throws IOException {
    Thread currentThread = Thread.currentThread();
    ClassLoader contextClassLoader = currentThread.getContextClassLoader();
    try {
      List<URL> runtimeUrls = createPathUrls(sourceDir);
      getLog().info("IDL Compile classpath: " + runtimeUrls);
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
    List<URL> runtimeUrls = new ArrayList<>();
    runtimeUrls.add(sourceFolder.toURI().toURL());
    // If runtimeClasspathElements is not empty values add its values to Idl path.
    if (cpElements != null && !cpElements.isEmpty()) {
      for (String element : cpElements) {
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
    // hack uses the same logic and Idl...
    String idlSource = new File(".").toURI().relativize(idl.toURI()).toString();
    Log log = getLog();
    log.debug("Injecting mvnIds to " + idlSource);
    for (Schema s : protocol.getTypes()) {
      if (s.getProp("mvnId") != null) {
        continue;
      }
      String sourceIdl = s.getProp("sourceIdl");
      if (sourceIdl == null) {
        log.warn("sourceIdl not available, will not attach mvnId for IDLs");
        continue;
      }
      SourceLocation sl = new SourceLocation(sourceIdl);
      if (!idlSource.equals(sl.getFilePath())) {
        continue;
      }
      int zbLineNr = sl.getLineNr() - 1;
      String line = readAllLines.get(zbLineNr);
      String sMvnId = genMnvId(s);
      log.debug("inserting mvnId: " + sMvnId + " at "
              + sl + " for line \"" + line + "\" schema: " + s.getFullName());
      int zbColNr = sl.getColNr() - 1;
      readAllLines.set(zbLineNr, line.substring(0, zbColNr)
              + " @mvnId(\"" + sMvnId + "\") "
              + line.substring(zbColNr, line.length()));
    }
    Path tempIdl = Files.createTempFile(this.target.toPath(), idl.getName(), ".tmp");
    Files.write(tempIdl, readAllLines, charset);
    return tempIdl.toFile();
  }


  protected void doCompileSchemas(final String[] filenames)
          throws IOException {
    getLog().debug("Compiling: " + Arrays.toString(filenames) + ", from " + sourceDirectory);
    ClassLoader avroLibClassLoader = org.apache.avro.Schema.class.getClassLoader();
    Thread currentThread = Thread.currentThread();
    ClassLoader contextClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(avroLibClassLoader);
    try {
    ExtendedParser parser = new UnresolvedExtendedParser();
    Map<String, Schema> schemas = Maps.newHashMapWithExpectedSize(filenames.length);
    Map<String, File> srcFiles = Maps.newHashMapWithExpectedSize(filenames.length);
    for (String fileName : filenames) {
      File src = new File(sourceDirectory, fileName);
      Schema schema = parser.parse(src);
      if (addMavenId) {
        attachMavenId(schema);
      }
      String fullName = schema.getFullName();
      schemas.put(fullName, schema);
      srcFiles.put(fullName, src);
    }
    List<Schema> schemaList = org.apache.avro.avsc.SchemaResolver.resolve(schemas,
            Boolean.getBoolean("allowUndefinedLogicalTypes"));
    for (Schema schema : schemaList) {
      writeSchemaToTarget(schema, srcFiles.get(schema.getFullName()));
    }
    } finally {
      currentThread.setContextClassLoader(contextClassLoader);
    }
  }

  private void writeSchemaToTarget(final Schema schema, final File src) throws IOException {
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
    try {
      loadPrevReleaseId2Map();
    } catch (IOException ex) {
      throw new MojoExecutionException("Unable to proces previous release of "
              + getMavenProject().getVersion(), ex);
    }
    super.execute();
    Log logger = this.getLog();
    logger.info("Generating java code + schemas, using avro "
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
        Arrays.sort(sourceFiles); // make this predictable, (mostly easier to test)
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
        Path indexFile = generatedAvscTargetPath.resolve(SCHEMA_INDEX_FILENAME);
        try (BufferedWriter bw = Files.newBufferedWriter(indexFile,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
          bw.append(SCHEMA_INDEX_PGK_KEY);
          bw.append('=');
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
