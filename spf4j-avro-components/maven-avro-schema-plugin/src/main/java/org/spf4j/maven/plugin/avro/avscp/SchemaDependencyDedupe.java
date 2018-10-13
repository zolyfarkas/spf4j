package org.spf4j.maven.plugin.avro.avscp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "avro-postcompile-cleanup", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public final class SchemaDependencyDedupe
        extends SchemaMojoBase {



  public void deleteGeneratedAvailableInDependencies() throws IOException {
    Path classesInfo = dependenciesDirectory.toPath().resolve("classes.txt");
    Set<String> classes = new HashSet(Files.readAllLines(classesInfo, StandardCharsets.UTF_8));
    Path javaPath = target.toPath().resolve("classes");
    List<Path> dupes = Files.walk(javaPath)
            .filter((p) -> {
              Path relativize = javaPath.relativize(p);
              return classes.contains(relativize.toString());
            }).collect(Collectors.toList());
    for (Path p : dupes) {
      Files.delete(p);
    }
    getLog().info("Deleted dupes: " + dupes);
  }

  public void deleteSchemasAvailableInDependencies() throws IOException {
    Path classesInfo = dependenciesDirectory.toPath();
    Set<Path> schemas = Files.walk(classesInfo).filter(
            (p) -> {
              Path fileName = p.getFileName();
              return fileName == null ? false : fileName.toString().endsWith("avsc");
            })
            .map((p) -> classesInfo.relativize(p)).collect(Collectors.toSet());
    Path schTargetPath = this.generatedAvscTarget.toPath();
    List<Path> dupes = Files.walk(schTargetPath).filter((p) -> schemas.contains(schTargetPath.relativize(p)))
            .collect(Collectors.toList());
    for (Path p : dupes) {
      Files.delete(p);
    }
    getLog().info("Deleted dupes: " + dupes);
  }


  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Deleting duplications from dependencies");
    try {
      deleteGeneratedAvailableInDependencies();
      deleteSchemasAvailableInDependencies();
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot cleanup generated dupes in " + this, ex);
    }

  }




}
