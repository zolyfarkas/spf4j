package org.spf4j.maven.plugin.avro.avscp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.spf4j.io.compress.Compress;

/**
 * mojo that packages a schema package and avro sources and attaches them as separate artifacts.
 */
@Mojo(name = "avro-package", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
public final class SchemaPackageMojo extends SchemaMojoBase {



  public String[] getSourceFiles() {
    FileSetManager fsm = new FileSetManager();
    FileSet fs = new FileSet();
    fs.setDirectory(sourceDirectory.getAbsolutePath());
    fs.addInclude("**/*.avsc");
    fs.addInclude("**/*.avpr");
    fs.addInclude("**/*.avdl");
    fs.setFollowSymlinks(false);
    return fsm.getIncludedFiles(fs);
  }

  /**
   * {@inheritDoc} running packaging of the current project may package a script for execution Dependencies libraries
   * are copied in the
   *
   *
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
    super.execute();
    Log logger = this.getLog();
    logger.info("Packaging schemas");
    Path avsc = target.toPath().resolve(
              mavenProject.getArtifactId() + '-' + mavenProject.getVersion() + '-' + "avsc." + schemaArtifactExtension);
    Path sourcePath = this.sourceDirectory.toPath();
    try {
      if (Files.notExists(sourcePath)) {
        return;
      }
      Compress.zip(this.generatedAvscTarget.toPath(), avsc);
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot package schemas and sources from " + this.generatedAvscTarget, ex);
    }
    DefaultArtifact schemas = new DefaultArtifact(mavenProject.getGroupId(),
            mavenProject.getArtifactId(), mavenProject.getVersion(), "compile",
            schemaArtifactExtension, schemaArtifactClassifier,
            new DefaultArtifactHandler(schemaArtifactExtension));
    schemas.setFile(avsc.toFile());
    logger.debug("Attaching " + schemas  + " from " + avsc);
    mavenProject.getAttachedArtifacts().add(schemas);
    Path sources = target.toPath().resolve(
              mavenProject.getArtifactId() + '-' + mavenProject.getVersion() + '-' + "avroSources.jar");
    try {
      Compress.zip(sourcePath, sources);
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot package sources from " + this.sourceDirectory, ex);
    }
    DefaultArtifact avroSources = new DefaultArtifact(mavenProject.getGroupId(),
            mavenProject.getArtifactId(), mavenProject.getVersion(), "compile", "jar", "avroSources",
            new DefaultArtifactHandler("jar"));
    avroSources.setFile(sources.toFile());
    mavenProject.getAttachedArtifacts().add(avroSources);
  }

  @Override
  public String toString() {
    return "SchemaPackageMojo{" + "schemaArtifactClassifier=" + schemaArtifactClassifier + ", "
          + super.toString() + '}';
  }

}
