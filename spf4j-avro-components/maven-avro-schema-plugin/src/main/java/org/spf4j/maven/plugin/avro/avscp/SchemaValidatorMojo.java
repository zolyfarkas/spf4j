package org.spf4j.maven.plugin.avro.avscp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.spf4j.maven.plugin.avro.avscp.validation.Validator;
import org.spf4j.maven.plugin.avro.avscp.validation.Validators;

/**
 * Goal that packages a schema package and avro sources and attaches them as separate artifacts.
 */
@Mojo(name = "avro-validate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true)
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public final class SchemaValidatorMojo extends SchemaMojoBase {


  @Parameter(name = "excludes")
  private List<String> excludes = Collections.EMPTY_LIST;

  /**
   * {@inheritDoc} running packaging of the current project may package a script for execution Dependencies libraries
   * are copied in the
   *
   *
   */
  @SuppressFBWarnings("PCAIL_POSSIBLE_CONSTANT_ALLOCATION_IN_LOOP")
  public void execute() throws MojoExecutionException {
    Log logger = this.getLog();
    logger.info("Validating schemas");
    Validators validators = new Validators(excludes);
    for (String file : getSchemaFiles()) {
      try {
        File src = new File(generatedAvscTarget, file);
        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(src);
        Map<String, Validator.Result> vresult = validators.validate(schema);
        if (!vresult.isEmpty()) {
          logger.error("Schema validation failed for " + schema.getFullName());
          for (Map.Entry<String, Validator.Result> res : vresult.entrySet()) {
            Validator.Result vres = res.getValue();
            String vName = res.getKey();
            logger.error("Validator " + vName + " failed with error: " + vres.getValidationErrorMessage());
            Exception ex = vres.getValidationException();
            if (ex != null) {
              logger.error("Validator " + vName + " exception", ex);
            }
            throw new MojoExecutionException("Failed to validate " + schema.getFullName());
          }
        }
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

  @Override
  public String toString() {
    return "SchemaValidatorMojo{" + "excludes=" + excludes + '}';
  }

}
