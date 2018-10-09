package org.spf4j.maven.plugin.avro.avscp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.spf4j.io.compress.Compress;
import org.spf4j.maven.MavenRepositoryUtils;

@Mojo(name = "avro-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.INITIALIZE)
public final class SchemaDependenciesMojo
        extends SchemaMojoBase {

  /**
   * The current build mavenSession instance.
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession mavenSession;

  /**
   * The entry point to Aether, i.e. the component doing all the work.
   *
   * @component
   */
  @Component
  private RepositorySystem repoSystem;

  @Parameter(defaultValue = "${mojoExecution}", required = true, readonly = true)
  private MojoExecution mojoExecution;


  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = this.getLog();
    log.info("Resolving schema dependencies");
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
    for (File file : deps) {
      try {
        Compress.unzip(file.toPath(), dependenciesDirectory.toPath(), (Path p)
                -> p.endsWith("avsc") || p.endsWith("avpr") || p.endsWith("avdl"));
      } catch (IOException ex) {
        throw new MojoExecutionException("Cannot unzip " + file, ex);
      }
    }
    Artifact avro = mojoExecution.getMojoDescriptor().getPluginDescriptor()
            .getArtifactMap().get("org.apache.avro:avro");
    Dependency dependency = new Dependency();
    dependency.setArtifactId(avro.getArtifactId());
    dependency.setGroupId(avro.getGroupId());
    dependency.setVersion(avro.getVersion());
    dependency.setType(avro.getType());
    dependency.setScope("runtime");
    mavenProject.getDependencies().add(dependency);
    mavenProject.getArtifacts().add(avro);
  }


}
