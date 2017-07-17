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
package org.spf4j.base.asm;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;

/**
 *
 * @author zoly
 */
public final class Scanner {

  private Scanner() {
  }

  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // Findbugs does not like try with resources.
  public static List<Invocation> findUsages(final Supplier<InputStream> classSupplier,
          final Set<Method> invokedMethods)
          throws IOException {
    try (InputStream is = classSupplier.get()) {
      ClassReader reader = new ClassReader(is);
      List<Invocation> result = new ArrayList<>();
      reader.accept(new MethodInvocationClassVisitor(result, invokedMethods), 0);
      return result;
    }
  }

  public static List<Invocation> findUsages(final Class<?> clasz, final Set<Method> invokedMethods) {
    try {
      return findUsages(clasz.getClassLoader(), clasz.getName().replaceAll("\\.", "/") + ".class", invokedMethods);
    } catch (IOException ex) {
      NoClassDefFoundError toThrow = new NoClassDefFoundError("Cannot reload " + clasz);
      toThrow.addSuppressed(ex);
      throw toThrow;
    }
  }

  public static List<Invocation> findUsages(final ClassLoader cl,
          final String claszResourcePath, final Set<Method> invokedMethods) throws IOException {
      return findUsages(() -> new BufferedInputStream(cl.getResourceAsStream(claszResourcePath)), invokedMethods);
  }

  public static List<Invocation> findUsages(final String packageName, final Set<Method> invokedMethods)
          throws IOException {
    final ClassLoader cl = ClassLoader.getSystemClassLoader();
    ClassPath cp = ClassPath.from(cl);
    ImmutableSet<ClassPath.ClassInfo> classes = cp.getAllClasses();
    List<Invocation> result = new ArrayList();
    for (ClassPath.ClassInfo info : classes) {
      if (info.getName().startsWith(packageName)) {
        result.addAll(findUsages(cl, info.getResourceName(), invokedMethods));
      }
    }
    return result;
  }

}
