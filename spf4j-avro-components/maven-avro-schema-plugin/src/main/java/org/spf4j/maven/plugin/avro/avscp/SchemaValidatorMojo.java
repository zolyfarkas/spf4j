package org.spf4j.maven.plugin.avro.avscp;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.Schema;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.spf4j.maven.plugin.avro.avscp.SchemaMojoBase;

/**
 * Goal that packages a schema package and avro sources and attaches them as separate artifacts.
 */
@Mojo(name = "avro-validate", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresProject = true)
public class SchemaValidatorMojo extends SchemaMojoBase {

  /**
   * {@inheritDoc} running packaging of the current project may package a script for execution Dependencies libraries
   * are copied in the
   *
   *
   */
  public void execute() throws MojoExecutionException {
    Log logger = this.getLog();
    logger.info("Validating schemas");
    Schema.Parser parser = new Schema.Parser();
    for (String file : getSchemaFiles()) {
      try {
        File src = new File(generatedAvscTarget, file);
        Schema schema = parser.parse(src);
      } catch (IOException ex) {
        throw new MojoExecutionException("Cannot validate " + file, ex);
      }
    }
  }

  public String[] getSchemaFiles() {
    FileSetManager fsm = new FileSetManager();
    FileSet fs = new FileSet();
    fs.setDirectory(generatedAvscTarget.getAbsolutePath());
    fs.addInclude("**/*.avsc");
    fs.setFollowSymlinks(false);
    return fsm.getIncludedFiles(fs);
  }


}
