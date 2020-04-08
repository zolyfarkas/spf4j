/*
 * Copyright 2019 The Apache Software Foundation.
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
package org.apache.avro;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PMB_POSSIBLE_MEMORY_BLOAT")
public final class SchemaResolvers {

  private static final Map<String, SchemaResolver> REGISTERED_RESOLVERS=
      new ConcurrentHashMap<>();


  private static volatile SchemaResolver defaultResolver = null;

  static {
    REGISTERED_RESOLVERS.put("none", SchemaResolver.NONE);
    ServiceLoader<SchemaResolverRegistration> regs = ServiceLoader.load(SchemaResolverRegistration.class);
    for (SchemaResolverRegistration reg : regs) {
      SchemaResolver ex = register(reg.getName(), reg.getResolver());
      if (ex != null) {
        Logger.getLogger(SchemaResolvers.class.getName())
                .log(Level.WARNING, "Overwriting schema resolver {0} with {1}", new Object [] {ex, reg.getResolver()});
      }
    }
  }



  public static  SchemaResolver get(@Nullable final String name) {
    if (name == null) {
      return getDefault();
    }
    return REGISTERED_RESOLVERS.get(name);
  }

  public static  SchemaResolver register(@Nullable final String name, final SchemaResolver resolver) {
    if (name == null) {
      SchemaResolver result = defaultResolver;
      defaultResolver = resolver;
      return result;
    } else {
      return REGISTERED_RESOLVERS.put(name, resolver);
    }
  }

  public static  SchemaResolver registerDefault(final SchemaResolver resolver) {
    return defaultResolver = resolver;
  }

  @Nonnull
  public static SchemaResolver getDefault() {
    SchemaResolver res = defaultResolver;
    if (res == null) {
      return SchemaResolver.NONE;
    }
    return res;
  }

  private SchemaResolvers() { }

}
