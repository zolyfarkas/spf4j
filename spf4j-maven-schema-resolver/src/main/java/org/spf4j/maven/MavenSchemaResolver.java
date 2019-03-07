/*
 * Copyright 2019 SPF4J.
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
package org.spf4j.maven;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipError;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.SchemaResolvers;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.spf4j.avro.SchemaRef;
import static org.spf4j.maven.MavenRepositoryUtils.getRepositorySystem;
import static org.spf4j.maven.MavenRepositoryUtils.getRepositorySystemSession;

/**
 * @author Zoltan Farkas
 */
public final class MavenSchemaResolver implements SchemaResolver {

  private final RepositorySystem repoSystem;

  private final RepositorySystemSession repoSystemSession;

  private final List<RemoteRepository> remotes;

  private final String classifier;

  private final String extension;

  public MavenSchemaResolver(final RepositorySystem repoSystem, final RepositorySystemSession repoSystemSession,
          final List<RemoteRepository> remotes, final String classifier, final String extension) {
    this.repoSystem = repoSystem;
    this.repoSystemSession = repoSystemSession;
    this.remotes = remotes;
    this.classifier = classifier;
    this.extension = extension;
  }

  public MavenSchemaResolver(final List<RemoteRepository> repos,
          final File localRepo, final String classifier, final String extension) {
    RepositorySystem repositorySystem = getRepositorySystem();
    RepositorySystemSession repositorySystemSession = getRepositorySystemSession(repositorySystem, localRepo);
    this.repoSystem = repositorySystem;
    this.repoSystemSession = repositorySystemSession;
    this.remotes = repos;
    this.classifier = classifier;
    this.extension = extension;
  }

  @Override
  @SuppressFBWarnings("PCAIL_POSSIBLE_CONSTANT_ALLOCATION_IN_LOOP")
  public Schema resolveSchema(final String id) {
    SchemaRef ref = new SchemaRef(id);
    try {
      File artifact = MavenRepositoryUtils.resolveArtifact(ref.getGroupId(), ref.getArtifactId(),
              classifier, extension, ref.getVersion(), remotes, repoSystem, repoSystemSession);
      URI zipUri = URI.create("jar:" + artifact.toURI().toURL());
      FileSystem zipFs;
      synchronized (zipUri.toString().intern()) { // newFileSystem fails if already one there...
        try {
          zipFs = FileSystems.newFileSystem(zipUri, Collections.emptyMap());
        } catch (FileSystemAlreadyExistsException ex) {
          zipFs = FileSystems.getFileSystem(zipUri);
        } catch (ZipError ze) {
          throw new AvroRuntimeException("Cannot resolve " + id, ze);
        }
      }
      for (Path root : zipFs.getRootDirectories()) {
        Path index = root.resolve("schema_index.properties");
        if (Files.exists(index)) {
          Properties prop = new Properties();
          try (BufferedReader indexReader = Files.newBufferedReader(index)) {
            prop.load(indexReader);
          }
          String schemaName = prop.getProperty(ref.getRef());
          if (schemaName == null) {
            throw new AvroRuntimeException("unable to resolve schema: " + id + " missing from index " + index);
          }
          Path schemaPath = root.resolve(schemaName.replace('.', '/') + ".avsc");
          try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(schemaPath))) {
            return new Schema.Parser().parse(bis);
          }
        }
      }
      throw new IOException("unable to resolve schema: " + id);

    } catch (ArtifactResolutionException | IOException ex) {
      throw new AvroRuntimeException("Cannot resolve " + id, ex);
    }
  }

  @Override
  public String getId(final Schema schema) {
    return schema.getProp("mvnId");
  }

  @Override
  public void registerAsDefault() {
    SchemaResolvers.registerDefault(this);
  }
  
  @Override
  public String toString() {
    return "MavenSchemaResolver{" + "repoSystem=" + repoSystem + ", repoSystemSession=" + repoSystemSession
            + ", remotes=" + remotes + ", classifier=" + classifier + ", extension=" + extension + '}';
  }



}
