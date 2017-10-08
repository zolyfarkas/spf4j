package org.spf4j.jdiff;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;

/**
 * Generates an API difference report between Java sources of two SCM versions
 */
@Mojo(name = "jdiff", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.VERIFY)
public final class ApiChangesMojo
        extends BaseJDiffMojo
        implements MavenReport {

  /**
   * The version range to compute diff for.
   * THis is a standard maven version range like [v1,v2], etc
   */
  @Parameter(property = "versionRange", defaultValue = "[,${project.version}]")
  private String versionRange;

  /**
   * The name of the destination directory.
   */
  @Parameter(property = "maxNumberOfDiffs", defaultValue = "10")
  private int maxNumberOfDiffs;

  /**
   * The name of the destination directory.
   */
  @Parameter(property = "destDir", defaultValue = "${project.reporting.outputDirectory}/apidocs")
  private File destDir;

  @Override
  public String getCategoryName() {
    return MavenReport.CATEGORY_PROJECT_REPORTS;
  }

  @Override
  public File getReportOutputDirectory() {
    return destDir;
  }

  @Override
  public boolean canGenerateReport() {
    return !getCompileSourceRoots().isEmpty();
  }

  @Override
  public String getOutputName() {
    return "apidocs/changes";
  }

  public String getName(final Locale locale) {
    return "JDiff Reports";
  }

  /**
   * This method is called when the report generation is invoked by maven-site-plugin.
   *
   * @param aSink
   * @param aSinkFactory
   * @param aLocale
   * @throws MavenReportException
   */
  public void generate(final Sink aSink, final SinkFactory aSinkFactory, final Locale aLocale)
          throws MavenReportException {
    if (!canGenerateReport()) {
      getLog().info("This report cannot be generated as part of the current build. "
              + "The report name should be referenced in this line of output.");
      return;
    }
    try {
      execute();
    } catch (MojoExecutionException ex) {
      throw new MavenReportException("Failed to exec report: " + this, ex);
    }
  }

  @Override
  public void execute() throws MojoExecutionException {
    MavenProject mavenProject = getMavenProject();
    try {
      getLog().info("Executing JDiff javadoc doclet");
      JDiffRunner runner = new JDiffRunner(getMojoExecution(), getToolchainManager(), getMavenSession(),
              getProjectRepos(), getRepoSystem(), getJavadocExecutable());
      runner.runDiffBetweenReleases(mavenProject.getGroupId(), mavenProject.getArtifactId(), this.versionRange,
              destDir, maxNumberOfDiffs);
      runner.writeChangesIndexHtml(destDir, "changes.html");
      getLog().info("Generated " + destDir + File.separatorChar + "changes.html");

    } catch (IOException | DependencyResolutionException | VersionRangeResolutionException
            | ArtifactResolutionException | JavadocExecutionException ex) {
      throw new MojoExecutionException("Failed executing mojo " + this, ex);
    }

  }


  @Override
  public void generate(final org.codehaus.doxia.sink.Sink sink, final Locale locale) throws MavenReportException {
    try {
      execute();
    } catch (MojoExecutionException ex) {
      throw new MavenReportException("Failed to exec report: " + this, ex);
    }
  }

  @Override
  public String getDescription(final Locale locale) {
    return "API JDiff reports";
  }

  @Override
  public void setReportOutputDirectory(final File outputDirectory) {
    this.destDir = new File(outputDirectory, "apidocs");
  }

  @Override
  public boolean isExternalReport() {
    return true;
  }

  @Override
  public String toString() {
    return "ApiChangesMojo{" + "versionRange=" + versionRange + ", maxNumberOfDiffs="
            + maxNumberOfDiffs + ", destDir=" + destDir  + ", parent=" + super.toString() + '}';
  }

}
