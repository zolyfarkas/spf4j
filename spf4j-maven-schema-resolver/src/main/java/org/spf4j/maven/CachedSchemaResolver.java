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
package org.spf4j.maven;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;

/**
 * A caching schema resolver;
 * @author Zoltan Farkas
 */
public final class CachedSchemaResolver implements SchemaResolver {


  private final LoadingCache<String, Schema> memoryCache;

  private final SchemaResolver resolver;

  public CachedSchemaResolver(final SchemaResolver resolver) {
    this.resolver = resolver;
    this.memoryCache = CacheBuilder.newBuilder().weakKeys().build(new CacheLoaderImpl(resolver));
  }

  @Override
  public Schema resolveSchema(final String id) {
    return memoryCache.getUnchecked(id);
  }

  @Override
  public String getId(final Schema schema) {
    return resolver.getId(schema);
  }

  @Override
  public String toString() {
    return "CachedSchemaResolver{" + "memoryCache=" + memoryCache + ", resolver=" + resolver + '}';
  }

  private static class CacheLoaderImpl extends CacheLoader<String, Schema> {

    private final SchemaResolver resolver;

    CacheLoaderImpl(final SchemaResolver resolver) {
      this.resolver = resolver;
    }

    @Override
    public Schema load(final String key) {
      return resolver.resolveSchema(key);
    }
  }

}
