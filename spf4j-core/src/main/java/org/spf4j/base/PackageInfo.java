/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
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
package org.spf4j.base;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY") // calling Throwables.writeTo
public final class PackageInfo implements Writeable {

  public static final PackageInfo NONE = new PackageInfo(null, null);

  private static final LoadingCache<String, PackageInfo> CACHE = CacheBuilder.newBuilder()
          .weakKeys().weakValues().build(new CacheLoader<String, PackageInfo>() {

            @Override
            public PackageInfo load(final String key) {
              return getPackageInfoDirect(key);
            }
          });


  @SuppressWarnings("checkstyle:regexp")
  public static void errorNoPackageDetail(final String message, final Throwable t) {
    if (Boolean.getBoolean("spf4j.reportPackageDetailIssues")) {
      System.err.println(message);
      Throwables.writeTo(t, System.err, Throwables.PackageDetail.NONE);
    }
  }

  @Nonnull
  public static PackageInfo getPackageInfoDirect(@Nonnull final String className) {
    Class<?> aClass;
    try {
      aClass = Class.forName(className);
    } catch (Throwable ex) { // NoClassDefFoundError if class fails during init.
      errorNoPackageDetail("Error getting package detail for " + className, ex);
      return NONE;
    }
    return getPackageInfoDirect(aClass);
  }

  @Nonnull
  public static PackageInfo getPackageInfoDirect(@Nonnull final Class<?> aClass) {
    URL jarSourceUrl = getJarSourceUrl(aClass);
    final Package aPackage = aClass.getPackage();
    if (aPackage == null) {
      return NONE;
    }
    String version = aPackage.getImplementationVersion();
    return new PackageInfo(jarSourceUrl, version);
  }

  @Nonnull
  public static PackageInfo getPackageInfo(@Nonnull final String className) {
    return CACHE.getUnchecked(className);
  }

  /**
   * Useful to get the jar URL where a particular class is located.
   *
   * @param clasz
   * @return
   */
  @Nullable
  public static URL getJarSourceUrl(final Class<?> clasz) {
    final CodeSource codeSource = clasz.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      return null;
    } else {
      return codeSource.getLocation();
    }
  }

  /**
   * the package location url.
   */
  private final URL url;
  /**
   * package version from manifest
   */
  private final java.lang.String version;

  public PackageInfo(@Nullable final URL url, @Nullable final String version) {
    this.url = url;
    this.version = version;
  }

  @Nullable
  public URL getUrl() {
    return url;
  }

  @Nullable
  public String getVersion() {
    return version;
  }
  
  public boolean hasVersion() {
    return this.version != null && !this.version.isEmpty();
  }

  public org.spf4j.base.avro.PackageInfo toAvro() {
    return new org.spf4j.base.avro.PackageInfo(this.url == null ? "" : this.url.toString(),
            this.version == null ? "" : version);
  }

  @Override
  public void writeTo(final Appendable appendable) throws IOException {
    if (url == null) {
      appendable.append("no_package_url");
    } else {
      appendable.append(url.toString());
    }
    if (version != null && !version.isEmpty()) {
      appendable.append('#');
      appendable.append(version);
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    this.writeTo(result);
    return result.toString();
  }
  
  

}
