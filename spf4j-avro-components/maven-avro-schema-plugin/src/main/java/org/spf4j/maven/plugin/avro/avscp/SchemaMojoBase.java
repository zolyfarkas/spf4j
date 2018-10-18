/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.maven.plugin.avro.avscp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;

/**
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
@SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE")
public abstract class SchemaMojoBase extends AbstractMojo {


  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject mavenProject;


  /**
   * The directory where all schema dependencies (avsc, avpr, avdl) are made vailable
   */
  @Parameter(name = "dependenciesDirectory",
          defaultValue = "${project.build.directory}/schema-dependencies", readonly = true)
  protected File dependenciesDirectory;

  /**
   * The source directory of avro files. This directory is added to the classpath at schema compiling time. All files
   * can therefore be referenced as classpath resources following the directory structure under the source directory.
   */
  @Parameter(name = "sourceDirectory", defaultValue = "${basedir}/src/main/avro")
  protected File sourceDirectory;


  /**
   *  the destination for the java generated files.
   */
  @Parameter(name = "generatedJavaTarget",
          defaultValue = "${project.build.directory}/generated-sources/avro")
  protected File generatedJavaTarget;


  /**
   * the destination for the avro schema json files.
   */
  @Parameter(name = "generatedAvscTarget",
          defaultValue = "${project.build.directory}/generated-sources/avsc")
  protected File generatedAvscTarget;


  /**
   *  the target folder.
   */
  @Parameter(name = "target", defaultValue = "${project.build.directory}")
  protected File target;

  /**
   * The current build mavenSession instance.
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  protected MavenSession mavenSession;

  /**
   * The entry point to Aether, i.e. the component doing all the work.
   */
  @Component
  protected  RepositorySystem repoSystem;

  public final RepositorySystem getRepoSystem() {
    return repoSystem;
  }

  public final MavenSession getMavenSession() {
    return mavenSession;
  }

  public final MavenProject getMavenProject() {
    return mavenProject;
  }

  public final File getGeneratedAvscTarget() {
    return generatedAvscTarget;
  }

  public final File getTarget() {
    return target;
  }

}
