/*
 * Copyright 2017 SPF4J.
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
package org.spf4j.jdiff;

import org.spf4j.jdiff.utils.Compress;
import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.Version;
import org.spf4j.maven.MavenRepositoryUtils;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
public final class JDiffRunner {

  private final String docletPath;

  private final String javadocExec;

  private final List<RemoteRepository> remoteRepos;

  private final RepositorySystem repositorySystem;
  private final RepositorySystemSession reposSession;

  public JDiffRunner() {
    this(null, null, null, Collections.singletonList(MavenRepositoryUtils.getDefaultRepository()),
            MavenRepositoryUtils.getRepositorySystem(), System.getProperty("spf4j.jdiff.javadocExec"));
  }

  @SuppressFBWarnings("STT_TOSTRING_STORED_IN_FIELD")
  public JDiffRunner(final MojoExecution mojoExec, final ToolchainManager toolchainManager,
          final MavenSession session,
          final List<RemoteRepository> remoteRepos,
          final RepositorySystem repositorySystem,
          final String javadocExec) {
    this.remoteRepos = remoteRepos;
    this.repositorySystem = repositorySystem;
    if (session != null) {
      this.reposSession = session.getRepositorySession();
    } else {
      this.reposSession = MavenRepositoryUtils.getRepositorySystemSession(
              MavenRepositoryUtils.getRepositorySystem(),
              new File(System.getProperty("user.home"), ".m2/repository"));
    }
    if (mojoExec != null) {
      StringBuilder cp = new StringBuilder(32);
      Map<String, Artifact> artifactMap = mojoExec.getMojoDescriptor().getPluginDescriptor().getArtifactMap();
      cp.append(artifactMap.get("jdiff:jdiff").getFile().getAbsolutePath());
      cp.append(File.pathSeparatorChar);
      cp.append(artifactMap.get("xerces:xercesImpl").getFile().getAbsolutePath());
      cp.append(File.pathSeparatorChar);
      docletPath = cp.toString();
    } else {
      try {
        Set<File> artf1 = MavenRepositoryUtils.resolveArtifactAndDependencies(
                "runtime", "jdiff", "jdiff", null, "jar", "1.0.9", remoteRepos, repositorySystem, reposSession);
        Set<File> artf2 = MavenRepositoryUtils.resolveArtifactAndDependencies(
                "runtime", "xerces", "xercesImpl", null, "jar", "2.10.0", remoteRepos, repositorySystem, reposSession);
        artf1.addAll(artf2);
        docletPath = MavenRepositoryUtils.toPath(artf1);
      } catch (DependencyResolutionException ex) {
        throw new UncheckedExecutionException(ex);
      }
    }

    if (javadocExec == null) {
      if (toolchainManager == null) {
        String home = System.getProperty("java.home");
        if (home.contains("jre")) {
          this.javadocExec = home + "/../bin/javadoc";
        } else {
          this.javadocExec = home + "/bin/javadoc";
        }
      } else {
        this.javadocExec = toolchainManager.getToolchainFromBuildContext("jdk", session).findTool("javadoc");
      }
    } else {
      this.javadocExec = javadocExec;
    }

  }

  public Set<String> generateJDiffXML(final Collection<File> sources,
          final Collection<File> classPath,
          final File destinationFolder,
          final String apiName,
          final Collection<String> includePackageNames)
          throws JavadocExecutionException, IOException {
    try {
      Files.createDirectories(destinationFolder.toPath());
      JavadocExecutor javadoc = new JavadocExecutor(javadocExec);

      javadoc.addArgumentPair("doclet", "jdiff.JDiff");
      javadoc.addArgumentPair("docletpath", docletPath);
      javadoc.addArgumentPair("apiname", apiName);
      javadoc.addArgumentPair("apidir", destinationFolder.getAbsolutePath());
      javadoc.addArgumentPair("classpath", MavenRepositoryUtils.toPath(classPath));
      javadoc.addArgumentPair("sourcepath", MavenRepositoryUtils.toPath(sources));

      Set<String> pckgs = new TreeSet<String>();

      if (includePackageNames != null && !includePackageNames.isEmpty()) {
        pckgs.addAll(includePackageNames);
      } else {
        pckgs = JDiffUtils.getPackages(sources);
      }

      for (String pckg : pckgs) {
        javadoc.addArgument(pckg);
      }
      javadoc.execute(destinationFolder);
      return pckgs;
    } catch (IOException e) {
      throw new JavadocExecutionException(e.getMessage(), e);
    }
  }

  public void generateReport(final File srcDir, final String oldApi, final String newApi,
          final String javadocOld, final String javadocNew, final Set<String> packages,
          final File destinationDir) throws JavadocExecutionException, IOException {
    Files.createDirectories(destinationDir.toPath());
    /**
     * javadoc -doclet jdiff.JDiff -docletpath ..\..\lib\jdiff.jar -d newdocs -stats -oldapi "SuperProduct 1.0" -newapi
     * "SuperProduct 2.0" -javadocold "../../olddocs/" -javadocnew "../../newdocs/" ..\..\lib\Null.java
     */
    JavadocExecutor javadoc = new JavadocExecutor(javadocExec);
    javadoc.addArgument("-private");
    javadoc.addArgumentPair("d", destinationDir.getAbsolutePath());
    String absolutePath = srcDir.getAbsolutePath();
    javadoc.addArgumentPair("oldapidir", absolutePath);
    javadoc.addArgumentPair("newapidir", absolutePath);
    if (javadocOld != null) {
      javadoc.addArgumentPair("javadocold", javadocOld);
    }
    if (javadocNew != null) {
      javadoc.addArgumentPair("javadocnew", javadocNew);
    }
    javadoc.addArgumentPair("doclet", "jdiff.JDiff");
    javadoc.addArgumentPair("docletpath", docletPath);
    javadoc.addArgumentPair("oldapi", oldApi);
    javadoc.addArgumentPair("newapi", newApi);
    javadoc.addArgument("-stats");
    for (String pckg : packages) {
      javadoc.addArgument(pckg);
    }
    javadoc.execute(destinationDir);
  }

  public void runDiffBetweenReleases(final String groupId, final String artifactId,
          final String versionRange, final File destinationFolder, final int maxNrOFDiffs)
          throws DependencyResolutionException, VersionRangeResolutionException,
          IOException, ArtifactResolutionException, JavadocExecutionException {
    JDiffRunner jdiff = new JDiffRunner();
    List<Version> rangeVersions = MavenRepositoryUtils.getVersions(
            groupId, artifactId, versionRange, remoteRepos, repositorySystem, reposSession);
    int size = rangeVersions.size();
    if (size < 2) {
      return;
    }
    LinkedList<Version> versions = new LinkedList<>();
    versions.add(rangeVersions.get(size - 1));
    for (int i = size - 2, j = 1; i >= 0 && j < maxNrOFDiffs; i--) {
      Version ver = rangeVersions.get(i);
      if (ver.toString().contains("SNAPSHOT")) {
        continue;
      }
      versions.addFirst(ver);
      j++;
    }
    Version v = versions.get(0);
    File prevSourcesArtifact = MavenRepositoryUtils.resolveArtifact(
            groupId, artifactId, "sources", "jar", v.toString(), remoteRepos, repositorySystem, reposSession);
    File prevJavaDocArtifact = MavenRepositoryUtils.resolveArtifact(
            groupId, artifactId, "javadoc", "jar", v.toString(), remoteRepos, repositorySystem, reposSession);
    Path tempDir = Files.createTempDirectory("jdiff");
    try {
      Path sourceDestination = tempDir.resolve(artifactId).resolve(v.toString()).resolve("sources");
      Compress.unzip(prevSourcesArtifact.toPath(), sourceDestination);
      Set<File> deps = MavenRepositoryUtils.resolveArtifactAndDependencies("compile", groupId, artifactId,
              null, "jar", v.toString(), remoteRepos, repositorySystem, reposSession);
      String prevApiName = artifactId + '-' + v;
      Set<String> prevPackages = jdiff.generateJDiffXML(Collections.singletonList(sourceDestination.toFile()),
              deps, destinationFolder, prevApiName, null);
      for (int i = 1, l = versions.size(); i < l; i++) {
        v = versions.get(i);
        File sourceArtifact = MavenRepositoryUtils.resolveArtifact(
                groupId, artifactId, "sources", "jar", v.toString(), remoteRepos, repositorySystem, reposSession);
        File javadocArtifact = MavenRepositoryUtils.resolveArtifact(
                groupId, artifactId, "javadoc", "jar", v.toString(), remoteRepos, repositorySystem, reposSession);
        sourceDestination =  tempDir.resolve(artifactId).resolve(v.toString()).resolve("sources");
        Compress.unzip(sourceArtifact.toPath(), sourceDestination);
        deps = MavenRepositoryUtils.resolveArtifactAndDependencies("compile", groupId, artifactId,
                null, "jar", v.toString(), remoteRepos, repositorySystem, reposSession);
        String apiName = artifactId + '-' + v;
        Set<String> packages = jdiff.generateJDiffXML(Collections.singletonList(sourceDestination.toFile()), deps,
                destinationFolder, apiName, null);
        prevPackages.addAll(packages);
        Path reportsDestination = destinationFolder.toPath().resolve(prevApiName + '_' + apiName);
        Compress.unzip(prevJavaDocArtifact.toPath(), reportsDestination.resolve(prevApiName));
        Compress.unzip(javadocArtifact.toPath(), reportsDestination.resolve(apiName));

        jdiff.generateReport(destinationFolder, prevApiName, apiName,
                "../" + prevApiName + '/', "../" + apiName + '/', prevPackages, reportsDestination.toFile());
        prevApiName = apiName;
        prevPackages = packages;
        prevJavaDocArtifact = javadocArtifact;
      }
    } finally {
      FileUtils.deleteDirectory(tempDir.toFile());
    }
  }

  public void runDiffBetweenReleases(final String groupId, final String artifactId,
          final String version1, final String version2, final File destinationFolder,
          final Set<String> includePackages)
          throws ArtifactResolutionException, DependencyResolutionException, IOException, JavadocExecutionException {
    JDiffRunner jdiff = new JDiffRunner();
    File prevSourcesArtifact = MavenRepositoryUtils.resolveArtifact(
            groupId, artifactId, "sources", "jar", version1, remoteRepos, repositorySystem, reposSession);
    File prevJavaDocArtifact = MavenRepositoryUtils.resolveArtifact(
            groupId, artifactId, "javadoc", "jar", version1, remoteRepos, repositorySystem, reposSession);
    Path tempDir = Files.createTempDirectory("jdiff");
    try {
      Path sourceDestination = tempDir.resolve(artifactId).resolve(version1).resolve("sources");
      Compress.unzip(prevSourcesArtifact.toPath(), sourceDestination);
      Set<File> deps = MavenRepositoryUtils.resolveArtifactAndDependencies("compile", groupId, artifactId,
              null, "jar", version1, remoteRepos, repositorySystem, reposSession);
      String prevApiName = artifactId + '-' + version1;
      Set<String> prevPackages = jdiff.generateJDiffXML(Collections.singletonList(sourceDestination.toFile()),
              deps, destinationFolder, prevApiName, includePackages);

      File sourceArtifact = MavenRepositoryUtils.resolveArtifact(
              groupId, artifactId, "sources", "jar", version2, remoteRepos, repositorySystem, reposSession);
      File javadocArtifact = MavenRepositoryUtils.resolveArtifact(
              groupId, artifactId, "javadoc", "jar", version2, remoteRepos, repositorySystem, reposSession);
      sourceDestination = tempDir.resolve(artifactId).resolve(version2).resolve("sources");
      Compress.unzip(sourceArtifact.toPath(), sourceDestination);
      deps = MavenRepositoryUtils.resolveArtifactAndDependencies("compile", groupId, artifactId,
              null, "jar", version2, remoteRepos, repositorySystem, reposSession);
      String apiName = artifactId + '-' + version2;
      Set<String> packages = jdiff.generateJDiffXML(Collections.singletonList(sourceDestination.toFile()), deps,
              destinationFolder, apiName, includePackages);
      prevPackages.addAll(packages);
      Compress.unzip(prevJavaDocArtifact.toPath(), destinationFolder.toPath().resolve(prevApiName));
      Compress.unzip(javadocArtifact.toPath(), destinationFolder.toPath().resolve(apiName));

      jdiff.generateReport(destinationFolder, prevApiName, apiName,
              "../" + prevApiName + '/', "../" + apiName + '/', prevPackages, destinationFolder);
    } finally {
      FileUtils.deleteDirectory(tempDir.toFile());
    }
  }

  public void writeChangesIndexHtml(final File reportOutputDirectory, final String fileName) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            Files.newOutputStream(reportOutputDirectory.toPath().resolve(fileName)),
            StandardCharsets.UTF_8))) {
      writer.append("<HTML>\n"
              + "<HEAD>\n"
              + "<TITLE>\n"
              + "API Differences Reports\n"
              + "</TITLE>\n"
              + "</HEAD>\n"
              + "<BODY>\n"
              + "<table summary=\"Api Difference Reports\""
              + " width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">");
      Path reportPath = reportOutputDirectory.toPath();
      try (Stream<Path> stream = Files.walk(reportPath, 2)) {
        stream.map((p) -> reportPath.relativize(p))
                .filter((p) -> p.getNameCount() > 1 && p.endsWith("changes.html"))
                .forEach((p) -> {
                  try {
                    writer.append("  <tr>\n"
                            + "  <td bgcolor=\"#FFFFCC\">\n"
                            + "    <font size=\"+1\"><a href=\"" + p + "\"> "
                            + p.getName(0).toString().replace("_", " to ") + " </a></font>\n"
                            + "  </td>\n"
                            + "  </tr>");
                  } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                  }
                });
      }
      writer.append("</TABLE>\n"
              + "</BODY>\n"
              + "</HTML>");
    }
  }

  @Override
  public String toString() {
    return "JDiffRunner{" + "docletPath=" + docletPath + ", javadocExec=" + javadocExec
            + ", remoteRepos=" + remoteRepos + ", repositorySystem=" + repositorySystem
            + ", reposSession=" + reposSession + '}';
  }



}
