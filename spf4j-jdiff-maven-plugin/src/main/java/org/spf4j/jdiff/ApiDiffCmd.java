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

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

/**
 *
 * @author Zoltan Farkas
 */
public final class ApiDiffCmd {

  private ApiDiffCmd() { }

  private static class Options {

    @Option(name = "-gId", usage = "maven artifact group id", required = true)
    private String groupId = "";

    @Option(name = "-aId", usage = "maven artifact id", required = true)
    private String artifactId = "";

    @Option(name = "-fromVersion", usage = "from version", required = true)
    private String fromVersion = "";

    @Option(name = "-toVersion", usage = "to version", required = true)
    private String toVersion = "";

    @Option(name = "-o", usage = "destination folder")
    private File destination = new File(".");


    @Option(name = "-p", usage = "packages list", handler = StringArrayOptionHandler.class)
    private String[] packages = {};

  }

  @SuppressWarnings("checkstyle:regexp")
  public static void main(final String[] args) throws DependencyResolutionException, VersionRangeResolutionException,
          IOException, ArtifactResolutionException, JavadocExecutionException {
    Options options = new Options();
    CmdLineParser parser = new CmdLineParser(options);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println("Error: " + e.getMessage() + "\nUsage:");
      parser.printUsage(System.err);
      System.exit(64);
    }
    JDiffRunner runner = new JDiffRunner();
    runner.runDiffBetweenReleases(options.groupId, options.artifactId, options.fromVersion,
            options.toVersion, options.destination, ImmutableSet.copyOf(options.packages));
    System.exit(0);
  }

}
