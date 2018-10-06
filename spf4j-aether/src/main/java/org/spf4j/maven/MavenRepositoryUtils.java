/*
 * Copyright 2017 SPF4J.
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

import com.google.common.collect.Sets;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * inspired by:
 * http://www.hascode.com/2017/09/downloading-maven-artifacts-from-a-pom-file-programmatically-with-eclipse-aether/
 *
 * @author Zoltan Farkas
 */
public final class MavenRepositoryUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MavenRepositoryUtils.class);

  private MavenRepositoryUtils() {
  }

  public static String toPath(final Collection<File> files) {
    StringBuilder result = new StringBuilder(files.size() * 20);
    Iterator<File> iterator = files.iterator();
    if (iterator.hasNext()) {
      result.append(iterator.next().getAbsolutePath());
    } else {
      return "";
    }
    while (iterator.hasNext()) {
      result.append(File.pathSeparatorChar);
      result.append(iterator.next().getAbsolutePath());
    }
    return StringUtils.quoteAndEscape(result.toString(), '\'');
  }

  @Nonnull
  public static RepositorySystem getRepositorySystem() {
    DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
    serviceLocator
            .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    serviceLocator.addService(TransporterFactory.class, FileTransporterFactory.class);
    serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    serviceLocator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(final Class<?> type, final Class<?> impl, final Throwable exception) {
        LOG.error("Error creating service {}, {}", new Object[]{type, impl, exception});
      }
    });
    RepositorySystem service = serviceLocator.getService(RepositorySystem.class);
    if (service == null) {
      throw new IllegalStateException("No repository system in " + serviceLocator);
    }
    return service;
  }

  public static RepositorySystemSession getRepositorySystemSession(final RepositorySystem system,
          final File localRepoPath) {
    DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
            .newSession();

    LocalRepository localRepository = new LocalRepository(localRepoPath);
    repositorySystemSession.setLocalRepositoryManager(
            system.newLocalRepositoryManager(repositorySystemSession, localRepository));

    repositorySystemSession.setRepositoryListener(new AbstractRepositoryListener() {
      @Override
      public void artifactDownloaded(final RepositoryEvent event) {
        LOG.info("Downloaded artifact {}", event);
      }

    });

    return repositorySystemSession;
  }

  public static RemoteRepository getDefaultRepository() {
    return new RemoteRepository.Builder("central", "default",
            System.getProperty("spf4j.jdiff.defaultMavenRepo", "http://central.maven.org/maven2/"))
            .build();
  }

  public static List<Version> getVersions(final List<RemoteRepository> repos,
          final File localRepo, final String groupId, final String artifactId, final String versionExpr)
          throws VersionRangeResolutionException {
    RepositorySystem repositorySystem = getRepositorySystem();
    RepositorySystemSession session = getRepositorySystemSession(repositorySystem, localRepo);
    return getVersions(groupId, artifactId, versionExpr, repos, repositorySystem, session);
  }

  public static List<Version> getVersions(final String groupId, final String artifactId, final String versionExpr,
          final List<RemoteRepository> repos,
          final RepositorySystem repositorySystem, final RepositorySystemSession session)
          throws VersionRangeResolutionException {
    Artifact artifact = new DefaultArtifact(groupId, artifactId, null, null, versionExpr);
    VersionRangeRequest request = new VersionRangeRequest(artifact, repos, null);
    VersionRangeResult versionResult = repositorySystem.resolveVersionRange(session, request);
    return versionResult.getVersions();
  }

  public static File resolveArtifact(final List<RemoteRepository> repos,
          final File localRepo, final String groupId, final String artifactId,
          final String classifier, final String extension, final String versionExpr)
          throws ArtifactResolutionException {
    RepositorySystem repositorySystem = getRepositorySystem();
    RepositorySystemSession session = getRepositorySystemSession(repositorySystem, localRepo);
    return resolveArtifact(groupId, artifactId, classifier, extension, versionExpr, repos, repositorySystem, session);
  }

  public static File resolveArtifact(final String groupId, final String artifactId,
          final String classifier, final String extension, final String versionExpr,
          final List<RemoteRepository> repos,
          final RepositorySystem repositorySystem, final RepositorySystemSession session)
          throws ArtifactResolutionException {
    Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, versionExpr);
    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(repos);
    ArtifactResult artifactResult = repositorySystem
            .resolveArtifact(session, artifactRequest);
    artifact = artifactResult.getArtifact();
    return artifact.getFile();
  }

  public static Set<File> resolveArtifactAndDependencies(final List<RemoteRepository> repos,
          final File localRepo,
          final String scope,
          final String groupId, final String artifactId,
          final String classifier, final String extension, final String versionExpr)
          throws DependencyResolutionException {
    RepositorySystem repositorySystem = getRepositorySystem();
    RepositorySystemSession session = getRepositorySystemSession(repositorySystem, localRepo);
    return resolveArtifactAndDependencies(scope, groupId, artifactId, classifier, extension, versionExpr,
            repos, repositorySystem, session);

  }

  public static Set<File> resolveArtifactAndDependencies(final String scope,
          final String groupId, final String artifactId,
          final String classifier, final String extension, final String versionExpr,
          final List<RemoteRepository> repos,
          final RepositorySystem repositorySystem,
          final RepositorySystemSession session) throws DependencyResolutionException {
    Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, versionExpr);
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, scope));
    collectRequest.setRepositories(repos);
    DependencyFilter dependencyFilter = DependencyFilterUtils.classpathFilter(scope);
    DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, dependencyFilter);
    DependencyResult depresult = repositorySystem
            .resolveDependencies(session, dependencyRequest);
    List<ArtifactResult> artifactResults = depresult.getArtifactResults();
    Set<File> result = Sets.newHashSetWithExpectedSize(artifactResults.size());
    for (ArtifactResult ar : artifactResults) {
      result.add(ar.getArtifact().getFile());
    }
    return result;
  }

}
