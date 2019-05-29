package org.spf4j.maven.plugin.avro.avscp;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.spf4j.io.compress.Compress;
import org.spf4j.maven.MavenRepositoryUtils;

/**
 * mojo that fetches all schema dependencies.
 * All avro files will be made available at "dependenciesDirectory"
 * @author Zoltan Farkas
 */
@Mojo(name = "avro-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(phase = LifecyclePhase.INITIALIZE)
public final class SchemaDependenciesMojo
        extends SchemaMojoBase {


  @Parameter(defaultValue = "${mojoExecution}", required = true, readonly = true)
  private MojoExecution mojoExecution;


  @Parameter(name = "excludes")
  private List<String> excludes = Collections.EMPTY_LIST;


  private static List<Pattern> getPatterns(final Collection<String> patterns) {
    List<Pattern> result = new ArrayList<>(patterns.size());
    for (String sp : patterns) {
      result.add(Pattern.compile(sp));
    }
    return result;
  }

  private static boolean matches(final Iterable<Pattern> patterns, final String what) {
    for (Pattern p : patterns) {
      if (p.matcher(what).matches()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    super.execute();
    Log log = this.getLog();
    log.info("Resolving schema dependencies");
    List<Pattern> patterns = getPatterns(excludes);
    Set<File> deps = new HashSet();
    RepositorySystemSession repositorySession = mavenSession.getRepositorySession();
    List<Dependency> dependencies = mavenProject.getDependencies();
    try {
      for (Dependency dep : dependencies) {
        deps.addAll(MavenRepositoryUtils.resolveArtifactAndDependencies("compile",
                dep.getGroupId(), dep.getArtifactId(),
                dep.getClassifier(), dep.getType(), dep.getVersion(),
                mavenProject.getRemoteProjectRepositories(),
                repoSystem, repositorySession));
      }
    } catch (DependencyResolutionException ex) {
      throw new MojoExecutionException("Cannot resolve dependencies for " + this, ex);
    }
    log.info("resolved schema dependencies: " + deps);
    Path dependenciesTargetPath = dependenciesDirectory.toPath();
    try {
      Files.createDirectories(dependenciesTargetPath);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    List<String> classes = new ArrayList<>(64);
    for (File file : deps) {
      try {
        if (file.isDirectory()) {
          FileUtils.copyDirectory(file, dependenciesTargetPath.toFile());
        } else {
          List<Path> unzip = Compress.unzip2(file.toPath(), dependenciesTargetPath, (Path p)
                  -> {
            Path fileName = p.getFileName();
            if (fileName == null) {
              return false;
            }
            String fn = fileName.toString();
            if (fn.endsWith("class")) {
              Path root = p.getRoot();
              Path relativize;
              if (root != null) {
                relativize = root.relativize(p);
              } else {
                relativize = p;
              }
              classes.add(relativize.toString());
            }
            if (matches(patterns, p.toString())) {
              return false;
            }
            return fn.endsWith("avsc") || fn.endsWith("avpr") || fn.endsWith("avdl");
          });
          if (!unzip.isEmpty()) {
            log.info("Found dependency schemas: " + unzip  + " from " + file);
          }
        }
      } catch (IOException ex) {
        throw new MojoExecutionException("Cannot unzip " + file, ex);
      }
    }
    Path classesInfo = dependenciesTargetPath.resolve("classes.txt");
    try {
      Files.write(classesInfo,
              classes, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot write " + classesInfo, ex);
    }

  }

  @Override
  public String toString() {
    return "SchemaDependenciesMojo{" + "mavenSession=" + mavenSession + ", repoSystem="
            + repoSystem + ", mojoExecution=" + mojoExecution + ", " + super.toString()  + '}';
  }

}
