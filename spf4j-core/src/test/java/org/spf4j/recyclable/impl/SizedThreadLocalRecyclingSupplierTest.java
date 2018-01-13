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
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.ReferenceType;
import org.spf4j.recyclable.SizedRecyclingSupplier;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class SizedThreadLocalRecyclingSupplierTest {

  @Test
  @SuppressFBWarnings({"PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS",
    "UTAO_JUNIT_ASSERTION_ODDITIES_USE_ASSERT_EQUALS"})
  public void testSupplier() {
    SizedThreadLocalRecyclingSupplier<byte[]> supplier = new SizedThreadLocalRecyclingSupplier<>(
            new SizedRecyclingSupplier.Factory<byte[]>() {

      @Override
      @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
      public byte[] create(final int size) {
        return new byte[size];
      }

      @Override
      public int size(final byte[] object) {
        return object.length;
      }
    }, ReferenceType.SOFT);

    byte[] get = supplier.get(8000);
    supplier.recycle(get);
    byte[] get2 = supplier.get(8000);
    supplier.recycle(get2);
    Assert.assertTrue(get == get2);

    get = supplier.get(8000);
    get2 = supplier.get(8000);
    Assert.assertTrue(get != get2);
    supplier.recycle(get);
    supplier.recycle(get2);

    get = supplier.get(4000);
    Assert.assertEquals(8000, get.length);
    supplier.recycle(get);

    get = supplier.get(10000);
    Assert.assertEquals(10000, get.length);
    supplier.recycle(get);

    get = supplier.get(8000);
    get2 = supplier.get(8000);
    Assert.assertTrue(get.length == 10000 || get2.length == 10000);

  }

}
