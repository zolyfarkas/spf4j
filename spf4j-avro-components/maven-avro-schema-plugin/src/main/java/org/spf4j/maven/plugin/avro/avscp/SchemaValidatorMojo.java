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
 * <p>Mojo that runs all Schema validators on this project schemas</p>
 *
 * <p>Built in validators:</p>
 *
 * <ul>
 * <li>docValidator (schema documentation),</li>
 * <li>compatibility (schema compatibility),</li>
 * <li>namesValidator (schema named types and field name validation)</li>
 * </ul>
 *
 * <p>By default validation issues will fail the build. This can be disabled at validator level with:</p>
 * <pre>
 * <code>
 * &lt;validatorConfigs&gt;
 *  &lt;[validatorName].failOnIssue&gt;&lt;/[validatorName].failOnIssue&gt;
 * &lt;/validatorConfigs&gt;
 * </code>
 * </pre>
 * <p>A particular validator can be sckipped at schema level with:</p>
 * <pre>
 *
 * {@literal @}ignoreValidators(["validatorname"])
 *
 * </pre>
 * <p>
 * Custom validators can be built and used. A custom validator,
 * will need to implement the org.spf4j.maven.plugin.avro.avscp.validation.Validator interface, and will be loaded via
 * the java Service Loader api.</p>
 *
 * @see org.spf4j.maven.plugin.avro.avscp.validation.Validator Interface to implement a custom validator.
 * @see org.spf4j.maven.plugin.avro.avscp.validation.impl Built in validator implementations.
 */
@Mojo(name = "avro-validate", defaultPhase = LifecyclePhase.TEST, requiresProject = true)
@SuppressFBWarnings({"PATH_TRAVERSAL_IN", "SCII_SPOILED_CHILD_INTERFACE_IMPLEMENTOR"})
public final class SchemaValidatorMojo extends SchemaMojoBase implements ValidatorMojo {

  /**
   * you can exclude certain validators from execution.
   */
  @Parameter(name = "excludeValidators")
  private List<String> excludeValidators = Collections.EMPTY_LIST;

  /**
   * You can configure validators, see individual validator doc (javadoc) for supported configuration keys.
   * @see org.spf4j.maven.plugin.avro.avscp.validation.impl
   */
  @Parameter(name = "validatorConfigs")
  private Map<String, String> validatorConfigs = Collections.EMPTY_MAP;

  @Override
  public Map<String, String> getValidatorConfigs() {
    return validatorConfigs;
  }

  @SuppressFBWarnings("PCAIL_POSSIBLE_CONSTANT_ALLOCATION_IN_LOOP")
  public void execute() throws MojoExecutionException {
    Log logger = this.getLog();
    logger.info("Validating schemas");
    synchronized (String.class) {
      String orig = System.getProperty("allowUndefinedLogicalTypes");
      try {
        System.setProperty("allowUndefinedLogicalTypes", "true");
        Validators validators = new Validators(excludeValidators);
        for (String file : getSchemaFiles()) {
          try {
            File src = new File(generatedAvscTarget, file);
            Schema.Parser parser = new Schema.Parser();
            Schema schema = parser.parse(src);
            Map<String, Validator.Result> vresult = validators.validate(schema, this);
            String sIdl = schema.getProp("sourceIdl");
            handleValidation(vresult, logger,
                    schema.getFullName() +  (sIdl != null ? " from " + sIdl : ""));
          } catch (IOException ex) {
            throw new MojoExecutionException("Cannot validate " + file, ex);
          }
        }
        try {
          Map<String, Validator.Result> vresult = validators.validate(null, this);
          handleValidation(vresult, logger, "project");
        } catch (IOException ex) {
          throw new MojoExecutionException("Cannot validate " + this, ex);
        }
      } finally {
        if (orig != null) {
          System.setProperty("allowUndefinedLogicalTypes", orig);
        }
      }
    }
  }

  public void handleValidation(final Map<String, Validator.Result> vresult, final Log logger, final String detail)
          throws MojoExecutionException {
    if (!vresult.isEmpty()) {
      logger.error("Schema validation failed for " + detail);
      for (Map.Entry<String, Validator.Result> res : vresult.entrySet()) {
        Validator.Result vres = res.getValue();
        String vName = res.getKey();
        logger.error("Validator " + vName + " failed with error: " + vres.getValidationErrorMessage());
        Exception ex = vres.getValidationException();
        if (ex != null) {
          logger.error("Validator " + vName + " exception", ex);
        }
        throw new MojoExecutionException("Failed to validate " + detail);
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
    return "SchemaValidatorMojo{" + "excludes=" + excludeValidators + ", " + super.toString() + '}';
  }

}
