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
package org.spf4j.concurrent;

import java.util.Arrays;

/**
 * allows to combine various resource limiters/semahores
 * @author Zoltan Farkas
 */
public final class CompoundPermitSupplier implements PermitSupplier {

  private final PermitSupplier[] suppliers;

  public CompoundPermitSupplier(final PermitSupplier... suppliers) {
    this.suppliers = suppliers;
  }

  @Override
  public boolean tryAcquire(final int nrPermits, final long deadlineNanos)
          throws InterruptedException {
    PermitSupplier[] acquired = new PermitSupplier[suppliers.length];
    int i = 0;
    for (PermitSupplier sem : suppliers) {
      boolean tryAcquire;
      try {
        tryAcquire = sem.tryAcquire(nrPermits, deadlineNanos);
      } catch (InterruptedException | RuntimeException ex) {
        returnPermits(acquired, i, nrPermits);
        throw ex;
      }
      if (tryAcquire) {
        acquired[i++] = sem;
      } else {
        returnPermits(acquired, i, nrPermits);
        return false;
      }
    }
    return true;
  }

  @Override
  public Acquisition tryAcquireEx(final int nrPermits, final long deadlineNanos) throws InterruptedException {
    PermitSupplier[] acquired = new PermitSupplier[suppliers.length];
    int i = 0;
    for (PermitSupplier sem : suppliers) {
      Acquisition tryAcquire;
      try {
        tryAcquire = sem.tryAcquireEx(nrPermits, deadlineNanos);
      } catch (InterruptedException | RuntimeException ex) {
        returnPermits(acquired, i, nrPermits);
        throw ex;
      }
      if (tryAcquire.isSuccess()) {
        acquired[i++] = sem;
      } else {
        returnPermits(acquired, i, nrPermits);
        return tryAcquire;
      }
    }
    return Acquisition.SUCCESS;
  }

  private static void returnPermits(final PermitSupplier[] pss, final int nr, final int nrPermits) {
    RuntimeException ex = null;
    for (int i = nr - 1; i >= 0; i--) {
      PermitSupplier sem = pss[i];
      try {
        sem.addPermits(nrPermits);
      } catch (RuntimeException e) {
        if (ex == null) {
          ex = e;
        } else {
          ex.addSuppressed(e);
        }
      }
    }
    if (ex != null) {
      throw ex;
    }
  }

  @Override
  public String toString() {
    return "CompoundPermitSupplier{" + "suppliers=" + Arrays.toString(suppliers) + '}';
  }

}
