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
package org.spf4j.recyclable.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nonnull;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this is not a thread safe implementation.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
public final class ObjectHolder<T> {

  private static final Logger LOG = LoggerFactory.getLogger(ObjectHolder.class);
  private T obj;
  private boolean borrowed;
  @Nonnull
  private final RecyclingSupplier.Factory<T> factory;

  ObjectHolder(final RecyclingSupplier.Factory<T> factory) {
    this.factory = factory;
    this.borrowed = false;
    this.obj = null;
  }

  ObjectHolder(final RecyclingSupplier.Factory<T> factory, final boolean lazy) throws ObjectCreationException {
    this(factory);
    if (!lazy) {
      obj = factory.create();
    }
  }

  ObjectHolder(final T object, @Nonnull final RecyclingSupplier.Factory<T> factory) {
    this.factory = factory;
    borrowed = false;
    this.obj = object;
  }

  public synchronized T getObj() {
    return obj;
  }

  @Nullable
  public synchronized T borrowOrCreateObjectIfPossible() throws ObjectCreationException {
    if (borrowed) {
      return null;
    }
    if (obj == null) {
      obj = factory.create();
    }
    borrowed = true;
    return obj;
  }

  @Nullable
  public synchronized T borrowObjectIfAvailable() {
    if (borrowed || obj == null) {
      return null;
    }
    borrowed = true;
    return obj;
  }

  public synchronized void returnObject(final T object, @Nullable final Exception e) {
    if (!borrowed || object != obj) {
      throw new IllegalStateException("Cannot return something that was "
              + "not borrowed from here " + object);
    }
    borrowed = false;
    if (e != null) {
      boolean isValid;
      Exception vex = null;
      try {
        isValid = factory.validate(object, e);
      } catch (Exception ex) {
        isValid = false;
        vex = ex;
      }
      if (!isValid) {
        LOG.warn("Validation of {} failed, detail {}", obj, vex, e);
        obj = null;
        try {
          factory.dispose(object);
        } catch (ObjectDisposeException ex1) {
          LOG.warn("Failed to dispose {}", object, ex1);
        } catch (RuntimeException ex1) {
          LOG.error("Failed to dispose {}", object, ex1);
        }
      }
    }
  }

  public synchronized void validateObjectIfNotBorrowed() throws ObjectDisposeException {
    if (!borrowed && obj != null) {
      boolean isValid;
      Exception vex = null;
      try {
        isValid = factory.validate(obj, null);
      } catch (RuntimeException ex) {
        isValid = false;
      } catch (Exception ex) {
        isValid = false;
      }
      if (!isValid) {
        LOG.warn("Validation of {} failed, detail {} ", obj, vex);
        T object = obj;
        obj = null;
        factory.dispose(object);
      }
    }
  }

  public synchronized boolean disposeIfNotBorrowed() throws ObjectDisposeException {
    if (borrowed) {
      return false;
    }
    if (obj != null) {
      try {
        factory.dispose(obj);
      } finally {
        obj = null;
      }
    }
    return true;
  }

  @Override
  public synchronized String toString() {
    return "ObjectHolder{" + "obj=" + obj + ", borrowed=" + borrowed + ", factory=" + factory + '}';
  }

  public RecyclingSupplier.Factory<T> getFactory() {
    return factory;
  }


}
