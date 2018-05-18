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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.Version;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class MavenRepositoryUtilsTest {

  private static final Logger LOG = LoggerFactory.getLogger(MavenRepositoryUtilsTest.class);

  @Test
  public void testRepositoryOperations()
          throws VersionRangeResolutionException, ArtifactResolutionException, DependencyResolutionException {
    File localRepo = new File(System.getProperty("user.home"), ".m2/repository");
    RemoteRepository mavenCentralRepository = MavenRepositoryUtils.getDefaultRepository();
    List<Version> versions = MavenRepositoryUtils.getVersions(Collections.singletonList(mavenCentralRepository),
            localRepo, "org.spf4j", "spf4j-core", "[8.3,]");
    LOG.debug("Versions = {}", versions);
    String oldest = versions.get(0).toString();
    Assert.assertEquals("8.3.1", oldest);
    versions = MavenRepositoryUtils.getVersions(Collections.singletonList(mavenCentralRepository), localRepo,
            "org.spf4j", "spf4j-core", "[,8.3.9-SNAPSHOT)");
    LOG.debug("Versions = {}", versions);
    File resolveArtifact = MavenRepositoryUtils.resolveArtifact(Collections.singletonList(mavenCentralRepository),
            localRepo, "org.spf4j", "spf4j-core", "sources", "jar", oldest);
    LOG.debug("Artifact file = {}", resolveArtifact);
    Assert.assertTrue(resolveArtifact.canRead());

    Set<File> deps = MavenRepositoryUtils.resolveArtifactAndDependencies(
            Collections.singletonList(mavenCentralRepository), localRepo, "compile",
            "jdiff", "jdiff", null, "jar", "1.0.9");
    LOG.debug("Dependency files = {}", deps);
  }

}
