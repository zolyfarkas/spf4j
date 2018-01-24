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
package org.spf4j.jdiff;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.security.Permission;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zoltan Farkas
 */
public class ApiDiffCmdTest {

  private static final Logger LOG = LoggerFactory.getLogger(ApiDiffCmdTest.class);

  private static SecurityManager original;

  @BeforeClass
  public static void init() {
    original = System.getSecurityManager();
    System.setSecurityManager(new NoExitSecurityManager());
  }

  @AfterClass
  public static void cleanup() {
    System.setSecurityManager(original);
  }

  @Test
  public void testCmdLine() throws DependencyResolutionException, VersionRangeResolutionException,
          IOException, ArtifactResolutionException, JavadocExecutionException {
    File dest = Files.createTempDir();
    try {
      ApiDiffCmd.main(new String[]{
        "-gId", "org.spf4j",
        "-aId", "spf4j-core",
        "-fromVersion", "8.3.4",
        "-toVersion", "8.3.5",
        "-o", dest.toString(),
        "-p", "org.spf4j.concurrent", "org.spf4j.reflect"
      });
    } catch (ExitException ex) {
      Assert.assertEquals(0, ex.getExitCode());
    }
    LOG.debug("Written diff reports to: {}", dest);
    Assert.assertTrue(new File(dest, "changes.html").exists());
  }

  public static final class NoExitSecurityManager extends SecurityManager {

    @Override
    public void checkPermission(final Permission perm) {
      // allow anything.
    }

    @Override
    public void checkPermission(final Permission perm, final Object context) {
      // allow anything.
    }

    @Override
    public void checkExit(final int status) {
      super.checkExit(status);
      throw new ExitException(status);
    }

  }

  public static final class ExitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int exitCode;

    public ExitException(final int exitCode) {
      super("Exited with " + exitCode);
      this.exitCode = exitCode;
    }

    public int getExitCode() {
      return exitCode;
    }

  }

}
