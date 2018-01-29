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
import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.net.URL;
import java.security.CodeSource;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Zoltan Farkas
 */
public final class PackageInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final LoadingCache<String, PackageInfo> CACHE = CacheBuilder.newBuilder()
          .weakKeys().weakValues().build(new CacheLoader<String, PackageInfo>() {

            @Override
            public PackageInfo load(final String key) {
              return getPackageInfoDirect(key);
            }
          });


  public static final PackageInfo NONE = new PackageInfo(null, null);

  private final String url;
  private final String version;

  @ConstructorProperties({"url", "version"})
  public PackageInfo(@Nullable final String url, @Nullable final String version) {
    this.url = url;
    this.version = version;
  }

  @Nullable
  public String getUrl() {
    return url;
  }

  @Nullable
  public String getVersion() {
    return version;
  }

  @Override
  @SuppressFBWarnings("DMI_BLOCKING_METHODS_ON_URL")
  public int hashCode() {
    if (url != null) {
      return url.hashCode();
    } else {
      return 0;
    }
  }

  public boolean hasInfo() {
    return url != null || version != null;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PackageInfo other = (PackageInfo) obj;
    if (!java.util.Objects.equals(this.url, other.url)) {
      return false;
    }
    return java.util.Objects.equals(this.version, other.version);
  }

  @Override
  public String toString() {
    return "PackageInfo{" + "url=" + url + ", version=" + version + '}';
  }

  @Nonnull
  public static PackageInfo getPackageInfoDirect(@Nonnull final String className) {
    Class<?> aClass;
    try {
      aClass = Class.forName(className);
    } catch (ClassNotFoundException | NoClassDefFoundError ex) { // NoClassDefFoundError if class fails during init.
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
    return new PackageInfo(jarSourceUrl == null ? "" : jarSourceUrl.toString(), version);
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



}
