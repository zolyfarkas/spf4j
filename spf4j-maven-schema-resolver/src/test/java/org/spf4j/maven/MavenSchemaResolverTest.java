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

import java.io.File;
import java.util.Collections;
import org.apache.avro.Schema;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Zoltan Farkas
 */
public class MavenSchemaResolverTest {

  @Test
  public void testSchemaResolution() {
    File localRepo = new File(System.getProperty("user.home"), ".m2/repository");
    RemoteRepository bintray = new RemoteRepository.Builder("central", "default",
            "https://dl.bintray.com/zolyfarkas/core")
            .build();

    MavenSchemaResolver resolver = new MavenSchemaResolver(Collections.singletonList(bintray),
            localRepo, null, "jar");

    String mvnId = "org.spf4j.avro:core-schema:0.2:6";

    Schema resolveSchema = resolver.resolveSchema(mvnId);
    Assert.assertEquals("ServiceError", resolveSchema.getName());
  }

}
