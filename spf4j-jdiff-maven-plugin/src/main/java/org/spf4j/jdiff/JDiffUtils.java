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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;

/**
 * Utility-class for this plugin.
 */
public final class JDiffUtils {

  private JDiffUtils() { }

  public static Set<String> getPackages(final Collection<File> compileSourceRoots) throws IOException {
    Set<String> packages = new HashSet<String>(64);
    for (File compileRoot : compileSourceRoots) {
      @SuppressWarnings("unchecked")
      List<String> files
              = FileUtils.getFileNames(compileRoot, "**/*.java", null, false);
      for (String file : files) {
        packages.add(FileUtils.dirname(file).replace(File.separatorChar, '.'));
      }
    }
    return packages;
  }
}
