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

import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 *
 * @author Zoltan Farkas
 */
public final class Registerer {

  private Registerer() { }

  public static void register(final RepositorySystem repoSystem, final RepositorySystemSession repoSystemSession,
          final List<RemoteRepository> remotes, final String classifier, final String extension) {
        new MavenSchemaResolver(repoSystem, repoSystemSession, remotes, classifier, extension).registerAsDefault();
  }
}
