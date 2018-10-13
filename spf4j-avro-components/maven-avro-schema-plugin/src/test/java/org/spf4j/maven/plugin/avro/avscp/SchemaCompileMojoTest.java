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
import java.io.IOException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zoltan Farkas
 */
public class SchemaCompileMojoTest {

  private static final Logger LOG  = LoggerFactory.getLogger(SchemaCompileMojoTest.class);

  @Test
  public void testPlugin() throws VerificationException, IOException {
    File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/test-schemas");
    LOG.debug("Building in {}", testDir);
    Verifier verifier = new Verifier(testDir.getAbsolutePath(), true);
    verifier.deleteArtifact("org.spf4j", "test-schema", "1.0-SNAPSHOT", "jar");
    verifier.executeGoal("install");
    verifier.verifyErrorFreeLog();
    verifier.resetStreams();
  }

}
