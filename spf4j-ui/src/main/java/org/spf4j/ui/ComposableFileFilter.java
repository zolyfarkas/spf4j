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
package org.spf4j.ui;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.swing.filechooser.FileFilter;
import org.spf4j.io.Csv;

/**
 * @author Zoltan Farkas
 */
public abstract class ComposableFileFilter extends FileFilter {

  public final ComposableFileFilter or(final FileFilter fileFilter) {
    return new ComposableFileFilter() {

      @Override
      public String getDescription() {
        StringBuilder sb = new StringBuilder();
        try {
          Csv.CSV.writeCsvRowNoEOL(sb, ComposableFileFilter.this.getDescription(), fileFilter.getDescription());
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
        return sb.toString();
      }

      @Override
      public boolean accept(final File f) {
        return ComposableFileFilter.this.accept(f) || fileFilter.accept(f);
      }

    };
  }

}
