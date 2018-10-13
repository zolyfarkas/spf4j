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

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
public abstract class SchemaMojoBase extends AbstractMojo {


  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject mavenProject;


  /**
   * The working directory for this plugin.
   */
  @Parameter(defaultValue = "${project.build.directory}/schema-dependencies", readonly = true)
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
  @Parameter(name = "target",
          defaultValue = "${project.build.directory}")
  protected File target;

}
