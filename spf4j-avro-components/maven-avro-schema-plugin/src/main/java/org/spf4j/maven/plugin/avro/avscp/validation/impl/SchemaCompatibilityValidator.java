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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.Version;
import org.spf4j.avro.schema.SchemaDiff;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.io.compress.Compress;
import org.spf4j.maven.MavenRepositoryUtils;
import org.spf4j.maven.plugin.avro.avscp.SchemaCompileMojo;
import org.spf4j.maven.plugin.avro.avscp.ValidatorMojo;
import org.spf4j.maven.plugin.avro.avscp.validation.Validator;

/**
 * Validates previously released schemas for backward compatibility.
 *
 * <p>The following configurations are available for this "compatibility" validator:</p>
 * <ul>
 * <li>versionRange - maven version range to check compatibility against. (defaults to
 * "[," + mavenProject.getVersion() +  ')' )</li>
 *
 * <li>maxNrOfVersionsToCheckForCompatibility = max number oof versions to check against.
 * (defaults to 30)</li>
 *
 * <li>maxNrOfDaysBackCheckForCompatibility - max released time to check against (defaults to 1 year)</li>
 *
 * <li>deprecationRemoval - validates  that a removed schema
 * has been previously deprecated (false by default)</li>
 *
 * <li>schemaArtifactClassifier - the classifier of the artifact that contains the schema files.
 * defaults to null (the main jar)</li>
 *
 * <li>schemaArtifactExtension - the extension of the artifact that contains the schema files.
 * defaults to jar </li>
 *
 * </ul>
 *
 * <p>The following validations are performed:</p>
 *
 * <ul>
 * <li>1) We check previously released schema for (reader to writer) and (writer to reader) compatibility
 * with the current schema. Unless compatibility is not desired ( via "beta" schema annotation,
 * or only partial compatibility is desired with  "noNewToOldCompatibility" "noOldToNewCompatibility"
 * schema annotations. (objects written with old schema don't need to be converted to new objects)
 * or "noNewToOldCompatibility" if (objects written with new schema don't need to be converted to old schema objects)
 * </li>
 *
 * <li>
 * 2) Schema deprecation and removal policy can also enabled with compatibiliy.deprecationRemoval = true,
 * this will validate that a schema has been deprecated during the entire compatibility interval.
 * </li>
 * </ul>
 * @author Zoltan Farkas
 */
public final class SchemaCompatibilityValidator implements Validator<Void> {

  @Override
  public String getName() {
    return "compatibility";
  }

  @Override
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public Result validate(final Void nv, final ValidatorMojo mojo) throws IOException {
    // loop through dependencies.
    MavenProject mavenProject = mojo.getMavenProject();
    Map<String, String> validatorConfigs = mojo.getValidatorConfigs();
    Log log = mojo.getLog();
    String versionRange = validatorConfigs.get("versionRange");
    if (versionRange == null) {
      versionRange = "[," + mavenProject.getVersion() +  ')';
    }
    int maxNrVersToCheck = 30;
    String strNrVer = validatorConfigs.get("maxNrOfVersionsToCheckForCompatibility");
    if (strNrVer != null) {
      maxNrVersToCheck = Integer.parseInt(strNrVer);
    }
    Instant instantToGoBack;
    String strNrDays = validatorConfigs.get("maxNrOfDaysBackCheckForCompatibility");
    if (strNrDays == null) {
      instantToGoBack = Instant.now().atOffset(ZoneOffset.UTC).minus(1,  ChronoUnit.YEARS).toInstant();
    } else {
      instantToGoBack = Instant.now().atOffset(ZoneOffset.UTC)
              .minus(Integer.parseInt(strNrDays), ChronoUnit.DAYS).toInstant();
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
    rangeVersions = rangeVersions.stream().filter((v) -> !v.toString().endsWith("SNAPSHOT"))
            .collect(Collectors.toList());
    int tSize = rangeVersions.size();
    rangeVersions = rangeVersions.subList(Math.max(tSize - maxNrVersToCheck, 0), tSize);
    log.info("Validating compatibility with previous versions " + rangeVersions + " newer than " + instantToGoBack);
    if (rangeVersions.isEmpty()) {
      return Result.valid();
    }
    String schemaArtifactClassifier = validatorConfigs.get("schemaArtifactClassifier");
    if (schemaArtifactClassifier != null && schemaArtifactClassifier.trim().isEmpty()) {
      schemaArtifactClassifier = null;
    }
    String schemaArtifactExtension = validatorConfigs.get("schemaArtifactExtension");
    if (schemaArtifactExtension == null || schemaArtifactExtension.trim().isEmpty()) {
      schemaArtifactExtension = "jar";
    }
    List<String> issues = new ArrayList<>(4);
    int size = rangeVersions.size();
    for (int  i = size - 1; i >= 0; i--) {
      Version version  = rangeVersions.get(i);
      validateCompatibility(groupId, artifactId, schemaArtifactClassifier, schemaArtifactExtension, version,
              remoteProjectRepositories, repoSystem, repositorySession, mojo,
              Boolean.parseBoolean(validatorConfigs.getOrDefault("deprecationRemoval", "true")),
              instantToGoBack, issues::add);
    }
    if (issues.isEmpty()) {
      return Result.valid();
    } else {
      return Result.failed("Schema compatibility issues:\n" + String.join("\n", issues));
    }
  }

  @SuppressFBWarnings("PCAIL_POSSIBLE_CONSTANT_ALLOCATION_IN_LOOP") // Schema.Parser is mutable, false positive.
  @SuppressWarnings("checkstyle:ParameterNumber") // valid will address later.
  public void validateCompatibility(final String groupId, final String artifactId,
          @Nullable final String classifier, final String extension,
          final Version version,
          final List<RemoteRepository> remoteProjectRepositories,
          final RepositorySystem repoSystem,
          final RepositorySystemSession repositorySession,
          final ValidatorMojo mojo,
          final boolean deprecationRemoval,
          final Instant instantToGoBack,
          final Consumer<String> issues)
          throws IOException {
    Log log = mojo.getLog();
    log.info("Validating compatibility with version: " + version + ", " + groupId + ':' + artifactId
            + ':' + classifier + ':' + extension);
    Path targetPath = mojo.getTarget().toPath();
    Path currSchemasPath = mojo.getGeneratedAvscTarget().toPath();
    File prevSchemaArchive;
    try {
      prevSchemaArchive = MavenRepositoryUtils.resolveArtifact(
              groupId, artifactId, classifier, extension, version.toString(),
              remoteProjectRepositories, repoSystem, repositorySession);
    } catch (ArtifactResolutionException ex) {
      throw new RuntimeException("Cannot resolve previous version "  + version, ex);
    }
    Path dest = targetPath.resolve("prevSchemas").resolve(version.toString());
    Files.createDirectories(dest);
    log.debug("Unzipping " + prevSchemaArchive + " to " + dest);
    List<Path> prevSchemas = Compress.unzip2(prevSchemaArchive.toPath(), dest, (Path p) -> {
      Path fileName = p.getFileName();
      if (fileName == null) {
        return false;
      }
      String fname = fileName.toString();
      return (fname.endsWith("avsc")
              || SchemaCompileMojo.SCHEMA_MANIFEST.equals(fname)
              || "MANIFEST.MF".equals(fname));
    });
    Instant dependencyBuildTime = getDependencyBuildTime(dest, log);
    if (dependencyBuildTime == null) {
      log.info("Package " + dest + " build time missing from manifest");
    } else if (dependencyBuildTime.isBefore(instantToGoBack)) {
      return;
    }
    for (Path prevSchemaPath : prevSchemas) {
      Path fileName = prevSchemaPath.getFileName();
      if (fileName == null) {
        continue;
      }
      if (!fileName.toString().endsWith("avsc")) {
        continue;
      }
      Path relPath = dest.relativize(prevSchemaPath);
      Path newSchemaPath = currSchemasPath.resolve(relPath);
      Schema previousSchema = new Schema.Parser().parse(prevSchemaPath.toFile());
      String previousSchemaName = previousSchema.getFullName();
      if (previousSchema.getProp("beta") != null) {
        log.debug("Skipping beta schema " + previousSchemaName);
        continue;
      }
      log.debug("Validating compatibility for " + previousSchemaName + " "
              + prevSchemaPath + " -> "  + newSchemaPath);
      if (!Files.exists(newSchemaPath)) {
        if (deprecationRemoval && previousSchema.getProp("beta") == null
                && previousSchema.getProp("deprecated") == null) {
          issues.accept(previousSchemaName + " is being removed without being deprecated first");
        }
      } else {
        Schema newSchema = new Schema.Parser().parse(newSchemaPath.toFile());
        validateCompatiblityBetween2Schemas(newSchema, previousSchema, version, issues, log, deprecationRemoval);
      }
    }
  }

  public void validateCompatiblityBetween2Schemas(final Schema newSchema, final Schema previousSchema,
          final Version previousVersion,
          final Consumer<String> issues, final Log log, final boolean deprecationRemoval) {
    if (newSchema.getProp("beta") != null) {
      log.debug("Skipping beta schema " + newSchema.getFullName() + " compatibility validation");
    } else {
      if (newSchema.getProp("noOldToNewCompatibility") == null) {
        SchemaCompatibility.SchemaPairCompatibility o2n
                = SchemaCompatibility.checkReaderWriterCompatibility(newSchema, previousSchema);
        if (o2n.getType() == SchemaCompatibility.SchemaCompatibilityType.INCOMPATIBLE) {
          issues.accept(newSchema.getFullName() + " cannot convert from previous version " + previousVersion
                 +  ", incompatibilities: " + o2n.getResult().getIncompatibilities()
                  + ",\n diff: \n" + diff(previousSchema, newSchema));
        }
      } else {
        log.debug("Skipping  schema " + newSchema.getFullName() + " noOldToNewCompatibility validation");
      }
      if (newSchema.getProp("noNewToOldCompatibility") == null) {
        SchemaCompatibility.SchemaPairCompatibility n2o
                = SchemaCompatibility.checkReaderWriterCompatibility(previousSchema, newSchema);
        if (n2o.getType() == SchemaCompatibility.SchemaCompatibilityType.INCOMPATIBLE) {
          issues.accept(newSchema.getFullName() + " cannot convert current to previos version " + previousVersion
                  +  ", incompatibilities: " + n2o.getResult().getIncompatibilities()
                  + ",\n diff: \n" + diff(previousSchema, newSchema));
        }
      } else {
        log.debug("Skipping  schema " + newSchema.getFullName() + " noNewToOldCompatibility validation");
      }
      if (deprecationRemoval) {
        Schemas.diff(previousSchema, newSchema, (diff) -> {
          if (diff.getDiffType() == SchemaDiff.Type.FIELD_MISSING_RIGHT)  {
            Schema.Field leftField = diff.getLeftField();
            if (leftField.getProp("deprecated") == null) {
              issues.accept(previousSchema.getFullName()  + " at " + diff.getPath()
                      + "field  " + leftField + " is being removed without being deprecated first");

            }
          }
        });
      }
    }
  }

  @Nullable
  private static Instant getDependencyBuildTime(final Path location, final Log log) throws IOException {
    Path jarManifest = location.resolve("META-INF/MANIFEST.MF");
    if (Files.exists(jarManifest)) {
      try (BufferedInputStream bis = new BufferedInputStream(
              Files.newInputStream(jarManifest))) {
        Manifest manifest = new Manifest(bis);
        Attributes mainAttributes = manifest.getMainAttributes();
        String buildTime = mainAttributes.getValue("Build-Time");
        if (buildTime != null) {
          try {
            return Instant.parse(buildTime);
          } catch (DateTimeParseException ex) {
            log.warn("Cannot parse manifest build time " + buildTime, ex);
          }
        }
      }
    }
    Path codegenManifest = location.resolve(SchemaCompileMojo.SCHEMA_MANIFEST);
    if (Files.exists(codegenManifest)) {
      try (BufferedReader br = Files.newBufferedReader(codegenManifest, StandardCharsets.UTF_8)) {
        Properties props = new Properties();
        props.load(br);
        String buildTime = props.getProperty("Build-Time");
        if (buildTime != null) {
          try {
            return Instant.parse(buildTime);
          } catch (DateTimeParseException ex) {
            log.warn("Cannot parse manifest build time " + buildTime, ex);
          }
        }
      }
    }
    return null;
  }

  static String diff(final Schema schema1, final Schema schema2) {
    String s1 = schema1.toString(true);
    String s2 = schema2.toString(true);
    DiffMatchPatch dmp = new DiffMatchPatch();
    LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(s1, s2, false);
    dmp.diffCleanupSemantic(diffs);
    StringBuilder result = new StringBuilder();
    for (DiffMatchPatch.Diff aDiff : diffs) {
      String text = aDiff.text;
      switch (aDiff.operation) {
        case INSERT:
          result.append("+<<<").append(text).append(">>>");
          break;
        case DELETE:
          result.append("-<<<").append(text).append(">>>");
          break;
        case EQUAL:
          result.append(text);
          break;
        default:
          throw new UnsupportedOperationException("Not supported " + aDiff.operation);
      }
    }
    return result.toString();
  }

  @Override
  public Class<Void> getValidationInput() {
    return Void.class;
  }

}
