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
import org.spf4j.recyclable.Disposable;
import org.spf4j.recyclable.ObjectBorower;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.RecyclingSupplier;
import java.util.Collection;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Either;
import org.spf4j.recyclable.ObjectDisposeException;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class SimpleSmartObjectPoolTest implements ObjectBorower<SimpleSmartObjectPoolTest.TestObject> {

  private static final Logger LOG = LoggerFactory.getLogger(SimpleSmartObjectPoolTest.class);

  private TestObject borowedObject = null;
  private final SimpleSmartObjectPool<TestObject> instance;

  public SimpleSmartObjectPoolTest() throws ObjectCreationException {
    instance = new SimpleSmartObjectPool(2, 10, new RecyclingSupplier.Factory<TestObject>() {
      @Override
      public TestObject create() {
        TestObject obj = new TestObject("Object");
        LOG.debug("Created {}", obj);
        return obj;
      }

      @Override
      public void dispose(final TestObject object) throws ObjectDisposeException {
        try {
          LOG.debug("Disposing Object {}", object);
          object.dispose();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }

      @Override
      public boolean validate(final TestObject object, final Exception e) {
        throw new UnsupportedOperationException("Not supported yet.");
      }

    }, true);
  }

  @Override
  @SuppressFBWarnings("MDM_THREAD_YIELD")
  public Either<Action, SimpleSmartObjectPoolTest.TestObject> tryRequestReturnObject() {
    if (borowedObject != null) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      instance.recycle(borowedObject, this);
    }
    return Either.left(Action.NONE);
  }

  @Override
  public TestObject tryReturnObjectIfNotInUse() {
    return borowedObject;
  }

  @Override
  public boolean scan(final ScanHandler<TestObject> handler) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Collection<TestObject> tryReturnObjectsIfNotNeededAnymore() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Collection<TestObject> tryReturnObjectsIfNotInUse() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean nevermind(final TestObject object) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public static final class TestObject implements Disposable {

    private boolean disposed = false;
    private final String data;

    public TestObject(final String data) {
      this.data = data;
    }

    public String getData() {
      if (!disposed) {
        return data;
      } else {
        throw new RuntimeException(data + " is already disposed");
      }
    }

    @Override
    public boolean tryDispose(final long timeoutMillis) {
      disposed = true;
      return true;
    }
  }

  /**
   * Test of get method, of class SimpleSmartObjectPool.
   */
  @Test
  public void testPool() throws InterruptedException, TimeoutException,
          ObjectCreationException, ObjectDisposeException {
    borowedObject = instance.get(this);
    Assert.assertNotNull(borowedObject);
    instance.recycle(borowedObject, this);
    instance.dispose();
  }
}
