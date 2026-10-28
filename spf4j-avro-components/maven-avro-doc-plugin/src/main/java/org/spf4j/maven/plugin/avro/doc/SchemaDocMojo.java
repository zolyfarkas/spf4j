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
package org.spf4j.maven.plugin.avro.doc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.avro.Schema;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Mojo that generates a single self-contained HTML documentation page for a set of avro schemas (*.avsc).
 * Pure java replacement for the unmaintained node.js based avrodoc tool (https://github.com/ept/avrodoc),
 * it removes the need for a node/npm install during the build.
 *
 * @author Zoltan Farkas
 */
@Mojo(name = "avro-doc", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresProject = true)
public final class SchemaDocMojo extends AbstractMojo {

  private static final Object PARSE_LOCK = new Object();

  /**
   * The directory containing the avro schema files (*.avsc) to document.
   */
  @Parameter(name = "avscDirectory", defaultValue = "${project.build.directory}/generated-sources/avsc")
  private File avscDirectory;

  /**
   * The generated HTML documentation file.
   */
  @Parameter(name = "docOutputFile", defaultValue = "${project.build.directory}/avrodoc.html")
  private File docOutputFile;

  /**
   * The documentation page title.
   */
  @Parameter(name = "docTitle", defaultValue = "${project.name}")
  private String docTitle;

  /**
   * Skip avro documentation generation. Bound to the skip.avrodoc property for compatibility
   * with the previous node.js based setup.
   */
  @Parameter(name = "skip", property = "skip.avrodoc", defaultValue = "false")
  private boolean skip;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = getLog();
    if (skip) {
      log.info("Skipping avro schema documentation generation");
      return;
    }
    Map<String, Schema> types = new LinkedHashMap<>();
    List<Path> schemaFiles = schemaFiles();
    for (Path avsc : schemaFiles) {
      try {
        Schema schema = parseSchema(avsc.toFile());
        AvroDocRenderer.collect(schema, types);
      } catch (IOException | RuntimeException ex) {
        throw new MojoExecutionException("Cannot parse schema " + avsc, ex);
      }
    }
    Path out = docOutputFile.toPath();
    try {
      Path parent = out.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
        AvroDocRenderer.render(writer, docTitle, types);
      }
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot write documentation to " + out, ex);
    }
    log.info("Generated " + out + " from " + schemaFiles.size() + " schemas, documenting " + types.size()
            + " named types with " + AvroDocRenderer.countFields(types) + " fields");
  }

  /**
   * Parse a schema file, tolerating undefined (custom) logical types: they are frequent in spf4j
   * schemas (for example the "any" logical type), and a documentation tool should render them,
   * not fail on them. The avro parser honors the allowUndefinedLogicalTypes system property,
   * which is set (and restored) around the parse. The property is JVM global, so the
   * set/parse/restore sequence is serialized: parallel module builds (mvn -T) must not
   * restore the property while another module is parsing.
   */
  @SuppressFBWarnings("PCAIL_POSSIBLE_CONSTANT_ALLOCATION_IN_LOOP") // a fresh parser per file is required,
  // schema files redefine the same named types
  static Schema parseSchema(final File avsc) throws IOException {
    synchronized (PARSE_LOCK) {
      String previous = System.getProperty("allowUndefinedLogicalTypes");
      System.setProperty("allowUndefinedLogicalTypes", "true");
      try {
        return new Schema.Parser().parse(avsc);
      } finally {
        if (previous == null) {
          System.clearProperty("allowUndefinedLogicalTypes");
        } else {
          System.setProperty("allowUndefinedLogicalTypes", previous);
        }
      }
    }
  }

  private List<Path> schemaFiles() throws MojoExecutionException {
    Path dir = avscDirectory.toPath();
    if (!Files.isDirectory(dir)) {
      getLog().warn("No avro schema directory " + dir + ", generating empty documentation");
      return Collections.emptyList();
    }
    try (Stream<Path> stream = Files.walk(dir)) {
      List<Path> files = stream
              .filter((p) -> {
                Path fileName = p.getFileName();
                return fileName != null && fileName.toString().endsWith(".avsc");
              })
              .collect(Collectors.toCollection(ArrayList::new));
      Collections.sort(files);
      return files;
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot scan schema directory " + dir, ex);
    }
  }

  @Override
  public String toString() {
    return "SchemaDocMojo{" + "avscDirectory=" + avscDirectory + ", docOutputFile=" + docOutputFile
            + ", docTitle=" + docTitle + ", skip=" + skip + '}';
  }

}
