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
package org.spf4j.avro;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.specific.SpecificRecord;
import org.spf4j.base.CloseableIterable;
import org.spf4j.base.Reflections;
import org.spf4j.security.AbacSecurityContext;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public interface AvroDataSet<T extends IndexedRecord> {

  enum Feature {
    FILTERABLE, PROJECTABLE
  }

  default Schema getElementSchema() {
    List<Type> intfs = Reflections.getImplementedGenericInterfaces(this.getClass());
    for (Type type : intfs) {
      TypeToken<?> tt = TypeToken.of(type);
      if (AvroDataSet.class.isAssignableFrom(tt.getRawType())) {
        if (type instanceof ParameterizedType) {
          Class<?> srClasz = TypeToken.of(((ParameterizedType) type).getActualTypeArguments()[0]).getRawType();
          if (SpecificRecord.class.isAssignableFrom(srClasz)) {
            try {
              return ((SpecificRecord) srClasz.getDeclaredConstructor().newInstance()).getSchema();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException | SecurityException ex) {
              throw new IllegalStateException("Invalid specific record " + srClasz, ex);
            }
          }
        } else {
          throw new IllegalStateException("Resource " + this + " must overwrite default implementation of getSchema()");
        }
      }
    }
    throw new IllegalStateException("Resource " + this + " must overwrite default implementation of getSchema()");
  }

  default String getName() {
    return getElementSchema().getName();
  }

  default Set<Feature> getFeatures() {
    return Collections.EMPTY_SET;
  }

  /**
   * @return -1 for unknown. or number of elements.
   */
  default long getRowCountStatistic() {
    return -1;
  }

  /**
   *
   * @param filter all results must comply to this filter, null means no filter.
   * @param selectProjections list of fields that are requested. null for no projections.
   * @return
   */
  CloseableIterable<? extends IndexedRecord> getData(@Nullable SqlPredicate<T> filter,
          @Nullable List<String> selectProjections,
          AbacSecurityContext secCtx,
          long timeout, TimeUnit timeUnit);


}
