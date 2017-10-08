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
import java.util.stream.Collectors;
import org.apache.maven.artifact.DependencyResolutionRequiredException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates an API descriptor of the Java sources.
 */
@Mojo(name = "descriptor", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.VERIFY)
public final class ApiDescriptorMojo
        extends BaseJDiffMojo {

  /**
   * The JDiff API name.
   */
  @Parameter(defaultValue = "${project.artifactId}-${project.version}")
  private String apiname;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.maven.plugin.AbstractMojo#execute()
   */
  public void execute()
          throws MojoExecutionException {
    try {
      JDiffRunner runner = new JDiffRunner(getMojoExecution(), getToolchainManager(), getMavenSession(),
              getProjectRepos(), getRepoSystem(), getJavadocExecutable());
      runner.generateJDiffXML(getCompileSourceRoots().stream()
              .map((s) -> new File(s)).collect(Collectors.toList()),
              getMavenProject().getCompileClasspathElements().stream()
                      .map((s) -> new File(s)).collect(Collectors.toList()), getWorkingDirectory(),
              apiname, getIncludePackageNames());
    } catch (IOException | DependencyResolutionRequiredException | JavadocExecutionException ex) {
      throw new MojoExecutionException("Cannot generated jdiff api xml, " + this, ex);
    }
  }

  @Override
  public String toString() {
    return "ApiDescriptorMojo{" + "apiname=" + apiname + ", parent=" + super.toString() +  '}';
  }



}
