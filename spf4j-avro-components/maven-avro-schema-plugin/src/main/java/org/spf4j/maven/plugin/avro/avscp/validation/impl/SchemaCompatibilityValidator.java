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
package org.spf4j.maven.plugin.avro.avscp.validation.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.Version;
import org.spf4j.io.compress.Compress;
import org.spf4j.maven.MavenRepositoryUtils;
import org.spf4j.maven.plugin.avro.avscp.ValidatorMojo;
import org.spf4j.maven.plugin.avro.avscp.validation.Validator;

/**
 * Validates all previously released schemas for backward compatibility.
 *
 * The following validations are performed:
 *
 * 1) When a schema published in a previous version is being removed,
 * we check that the previous released schema has been "deprecated". (deprecated attribute).
 *
 * 1) We check previously released schema for (reader -> writer) and (writer -> reader) compatibility
 * with the current schema. Unless compatibility is not desired (if schema has attribute "noCompatibility")
 * or only partial compatibility is desired "noOldToNewCompatibility"
 * (objects written with old schema don't need to be converted to new objects)
 * or "noNewToOldCompatibility" if (objects written with new schema don't need to be converted to old schema objects)
 *
 * @author Zoltan Farkas
 */
public final class SchemaCompatibilityValidator implements Validator<ValidatorMojo> {

  @Override
  public String getName() {
    return "compatibility";
  }

  @Override
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public Result validate(final ValidatorMojo mojo) throws IOException {
    // loop through dependencies.
    MavenProject mavenProject = mojo.getMavenProject();
    String versionRange = mojo.getValidatorConfigs().get("compatibiliy.versionRange");
    if (versionRange == null) {
      versionRange = "[," + mavenProject.getVersion() +  ')';
    }
    String groupId = mavenProject.getGroupId();
    String artifactId = mavenProject.getArtifactId();
    List<RemoteRepository> remoteProjectRepositories = mavenProject.getRemoteProjectRepositories();
    RepositorySystem repoSystem = mojo.getRepoSystem();
    RepositorySystemSession repositorySession = mojo.getMavenSession().getRepositorySession();
    List<Version> rangeVersions;
    try {
      rangeVersions = MavenRepositoryUtils.getVersions(groupId, artifactId, versionRange,
              remoteProjectRepositories, repoSystem, repositorySession);
    } catch (VersionRangeResolutionException ex) {
      throw new RuntimeException("Invalid compatibiliy.versionRange = " + versionRange + " setting", ex);
    }
    mojo.getLog().info("Validating compatibility with previous versions " + rangeVersions);
    if (rangeVersions.isEmpty()) {
      return Result.valid();
    }
    List<String> issues = new ArrayList<>(4);
    for (Version version : rangeVersions) {
      validateCompatibility(groupId, artifactId, version,
              remoteProjectRepositories, repoSystem, repositorySession, mojo, false, issues::add);
    }
    if (issues.isEmpty()) {
      return Result.valid();
    } else {
      return Result.failed("Schema compatibility issues:\n" + String.join("\n", issues));
    }
  }

  @SuppressFBWarnings("PCAIL_POSSIBLE_CONSTANT_ALLOCATION_IN_LOOP")
  public void validateCompatibility(final String groupId, final String artifactId, final Version version,
          final List<RemoteRepository> remoteProjectRepositories, final RepositorySystem repoSystem,
          final RepositorySystemSession repositorySession,
          final ValidatorMojo mojo, final boolean deprecationRemoval, final Consumer<String> issues)
          throws IOException {
    Log log = mojo.getLog();
    log.info("Validating compatibility with version: " + version);
    Path targetPath = mojo.getTarget().toPath();
    Path currSchemasPath = mojo.getGeneratedAvscTarget().toPath();
    File prevSchemaArchive;
    try {
      prevSchemaArchive = MavenRepositoryUtils.resolveArtifact(
              groupId, artifactId, "avsc", "jar", version.toString(),
              remoteProjectRepositories, repoSystem, repositorySession);
    } catch (ArtifactResolutionException ex) {
      throw new RuntimeException("Cannot resolve previous version "  + version, ex);
    }
    Path dest = targetPath.resolve("prevSchemas").resolve(version.toString());
    Files.createDirectories(dest);
    List<Path> prevSchemas = Compress.unzip(prevSchemaArchive.toPath(), dest);
    for (Path prevSchemaPath : prevSchemas) {
      Path relPath = dest.relativize(prevSchemaPath);
      Path newSchemaPath = currSchemasPath.resolve(relPath);
      Schema previousSchema = new Schema.Parser().parse(prevSchemaPath.toFile());
      String previousSchemaName = previousSchema.getFullName();
      log.info("Validating compatibility for " + previousSchemaName + " "
              + prevSchemaPath + " -> "  + newSchemaPath);
      if (deprecationRemoval && !Files.exists(newSchemaPath) && previousSchema.getProp("deprecated") == null) {
        issues.accept(previousSchemaName + " is being removed without being deprecated first");
      } else {
        Schema newSchema = new Schema.Parser().parse(newSchemaPath.toFile());
        SchemaCompatibility.SchemaPairCompatibility o2n
                = SchemaCompatibility.checkReaderWriterCompatibility(newSchema, previousSchema);
        if (o2n.getType() == SchemaCompatibility.SchemaCompatibilityType.INCOMPATIBLE
                && newSchema.getProp("noOldToNewCompatibility") == null) {
          issues.accept(newSchema.getFullName() + " cannot convert previous versions " + version + " to current"
                + " detail: " + o2n);
        }
        SchemaCompatibility.SchemaPairCompatibility n2o
                = SchemaCompatibility.checkReaderWriterCompatibility(previousSchema, newSchema);
        if (n2o.getType() == SchemaCompatibility.SchemaCompatibilityType.INCOMPATIBLE
                && newSchema.getProp("noNewToOldCompatibility") == null) {
          issues.accept(newSchema.getFullName() + " cannot convert current to previos version " + version
          + " detail: " + n2o);
        }

      }
    }
  }

  @Override
  public Class<ValidatorMojo> getValidationInput() {
    return ValidatorMojo.class;
  }

}
