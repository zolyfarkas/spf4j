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
package org.spf4j.recyclable;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.apache.commons.pool.PoolableObjectFactory;
import org.spf4j.recyclable.impl.ExpensiveTestObject;

/**
 *
 * @author zoly
 */
public final class ExpensiveTestObjectFactoryApache implements PoolableObjectFactory<ExpensiveTestObject> {

    private final long maxIdleMillis;
    private final int nrUsesToFailAfter;
    private final long  minOperationMillis;
    private final long maxOperationMillis;

    public ExpensiveTestObjectFactoryApache(final long maxIdleMillis, final int nrUsesToFailAfter,
            final long minOperationMillis, final long maxOperationMillis) {
        this.maxIdleMillis = maxIdleMillis;
        this.nrUsesToFailAfter = nrUsesToFailAfter;
        this.minOperationMillis = minOperationMillis;
        this.maxOperationMillis = maxOperationMillis;
    }

    public ExpensiveTestObjectFactoryApache() {
        this(100, 10, 1, 20);
    }


    @Override
    public ExpensiveTestObject makeObject() {
          return new ExpensiveTestObject(maxIdleMillis, nrUsesToFailAfter, minOperationMillis, maxOperationMillis);
    }

    @Override
    public void destroyObject(final ExpensiveTestObject t) throws IOException {
            t.close();
    }

    @Override
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_RETURN_FALSE")
    public boolean validateObject(final ExpensiveTestObject t) {
            try {
                t.doStuff();
                return true;
            } catch (IOException ex) {
                return false;
            }
    }

    @Override
    public void activateObject(final ExpensiveTestObject t) {
    }

    @Override
    public void passivateObject(final ExpensiveTestObject t) {
    }

  @Override
  public String toString() {
    return "ExpensiveTestObjectFactoryApache{" + "maxIdleMillis=" + maxIdleMillis
            + ", nrUsesToFailAfter=" + nrUsesToFailAfter + ", minOperationMillis="
            + minOperationMillis + ", maxOperationMillis=" + maxOperationMillis + '}';
  }



}
