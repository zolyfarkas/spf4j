package org.spf4j.jdiff;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

/**
 * Execute javadoc
 */
public final class JavadocExecutor {

  private Commandline cmd = new Commandline();

  public JavadocExecutor() {
    this(System.getProperty("java.home") + "/javadoc");
  }

  /**
   * The constructor
   *
   * @param executable the executable
   * @param log the mojo logger
   */
  public JavadocExecutor(final String executable) {
    cmd.setExecutable(executable);
  }

  /**
   * Add a javadoc argument pair
   *
   * @param argKey the key
   * @param argValue the value
   */
  public void addArgumentPair(final String argKey, final String argValue) {
    cmd.createArg().setValue("-" + argKey);

    cmd.createArg().setValue(argValue);
  }

  /**
   * Add an javadoc argument
   *
   * @param arg the argument
   */
  public void addArgument(final String arg) {
    cmd.createArg().setValue(arg);
  }

  /**
   * Execute from the {@code workingDir}
   *
   * @param workingDir the directory to execute the javadoc command from
   * @throws JavadocExecutionException if an exception occurs during the execution of javadoc or if that execution
   * doesn't exit with {@code 0}
   */
  public void execute(final File dir) throws JavadocExecutionException, IOException {
    if (!dir.exists()) {
      Files.createDirectories(dir.toPath());
    }

    cmd.setWorkingDirectory(dir.getAbsolutePath());

    int exitCode = 0;

    try {
      exitCode = CommandLineUtils.executeCommandLine(cmd,
              new DefaultConsumer(),
              new DefaultConsumer());
    } catch (Exception ex) {
      throw new JavadocExecutionException("generateJDiff doclet failed: " + cmd, ex);
    }

    if (exitCode != 0) {
      throw new JavadocExecutionException("generate JDiff doclet "
              + cmd + " failed with exit code "  + exitCode);
    }
  }

  @Override
  public String toString() {
    return "JavadocExecutor{" + "cmd=" + cmd + '}';
  }

}
