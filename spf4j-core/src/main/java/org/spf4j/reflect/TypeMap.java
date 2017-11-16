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
package org.spf4j.reflect;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Type to Object map.
 * association is not 1 - 1. if we have  type1 -> object that if type2 is subtype of type1, also type2 -> object
 * this is useful for resolving: ITC_INHERITANCE_TYPE_CHECKING
 *
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public interface TypeMap<H> extends ByTypeSupplier<H, RuntimeException> {

  /**
   * Get all Objects associated to all unrelated compatible types.
   *
   * for example we habe Object O of type T a subtype of T1 and T2.
   * if this typemap contains Objects mapped to T1 and T2, those 2 objects
   * will be returned if T1 and T2 are not related (subtypes of each other)
   * if T1 extends T2 the obeject mapped to the most specific type is returned.
   *
   * @param t
   * @return
   */
  @Nonnull
  Set<H> getAll(Type t);


  /**
   * get the object associated to a compatible type, only if there is only one.
   * @param t
   * @return
   */
  @Nullable
  @SuppressFBWarnings("SPP_USE_ISEMPTY")
  @Override
  default H get(final Type t) {
    Set<H> get = getAll(t);
    int size = get.size();
    if (size == 1) {
      return get.iterator().next();
    } else if (size == 0) {
      return null;
    } else {
      throw new IllegalArgumentException("Ambiguous handlers " + get + " for " + t + " in  " + this);
    }
  }

  /**
   * Get the the Object associated to type.
   * @param t
   * @return
   */
  @Nullable
  H getExact(Type t);


  /**
   * Associate object to type if no existing association present.
   * @param type
   * @param object
   * @return
   */
  @CheckReturnValue
  boolean putIfNotPresent(Type type, H object);

  /**
   * Associate object with type. if there is an existing association a exception will be thrown.
   * @param type
   * @param object
   */
  default TypeMap<H> safePut(final Type type, final H object) {
    if (!putIfNotPresent(type, object)) {
      throw new IllegalArgumentException("Cannot put " + type + ", " + object + " exiting mapping present");
    }
    return this;
  }

  /**
   * remove type association.
   * @param type
   * @return
   */
  @CheckReturnValue
  boolean remove(Type type);

}
