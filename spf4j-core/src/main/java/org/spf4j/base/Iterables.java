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

import java.util.function.Consumer;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Iterables {

  private Iterables() { }

  @Nullable
  @CheckReturnValue
  public static <T> RuntimeException forAll(final Iterable<T> itterable, final Consumer<? super T> consumer)  {
    MutableHolder<RuntimeException> hex = MutableHolder.of((RuntimeException) null);
    itterable.forEach(new Consumer<T>() {
      @Override
      public void accept(final T t) {
        try {
          consumer.accept(t);
        } catch (RuntimeException ex1) {
          RuntimeException ex = hex.getValue();
          if (ex == null) {
            hex.setValue(ex1);
          } else {
            hex.setValue(Throwables.suppress(ex1, ex));
          }
        }
      }
    });
    return hex.getValue();
  }

  public static <T> void forAll2(final Iterable<T> itterable, final Consumer<? super T> consumer)  {
    RuntimeException ex = forAll(itterable, consumer);
    if (ex != null) {
      throw ex;
    }
  }



}
