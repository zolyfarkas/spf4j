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
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.recyclable.ObjectBorrowException;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.RecyclingSupplier;

/**
 *
 * @author zoly
 */
public class SharingObjectPoolTest {

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testSharingPool()
          throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {

    SharingObjectPool<String> pool = createTestPool();

    String obj = pool.get();
    Assert.assertEquals("O0", obj);
    String obj2 = pool.get();
    Assert.assertEquals("O1", obj2);
    pool.recycle(obj);
    String obj3 = pool.get();
    Assert.assertEquals("O0", obj3);

  }

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testSharingPool2()
          throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {

    SharingObjectPool<String> pool = createTestPool();

    String obj = pool.get();
    Assert.assertEquals("O0", obj);
    pool.recycle(obj);
    String obj2 = pool.get();
    Assert.assertEquals("O0", obj2);
  }

  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  public static SharingObjectPool<String> createTestPool() throws ObjectCreationException {
    return new SharingObjectPool<>(new RecyclingSupplier.Factory<String>() {

      private int i = 0;

      @Override
      public String create() {
        return "O" + (i++);
      }

      @Override
      public void dispose(final String object) {
      }

      @Override
      public boolean validate(final String object, final Exception e) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    }, 0, 4);
  }

}
