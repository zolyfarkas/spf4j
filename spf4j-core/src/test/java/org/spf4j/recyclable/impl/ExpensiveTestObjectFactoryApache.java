 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.recyclable.impl;


import java.io.IOException;
import org.apache.commons.pool.PoolableObjectFactory;

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
    public ExpensiveTestObject makeObject() throws Exception {
          return new ExpensiveTestObject(maxIdleMillis, nrUsesToFailAfter, minOperationMillis, maxOperationMillis);
    }

    @Override
    public void destroyObject(final ExpensiveTestObject t) throws Exception {
            t.close();
    }

    @Override
    public boolean validateObject(final ExpensiveTestObject t) {
            try {
                t.doStuff();
                return true;
            } catch (IOException ex) {
                return false;
            }
    }

    @Override
    public void activateObject(final ExpensiveTestObject t) throws Exception {
    }

    @Override
    public void passivateObject(final ExpensiveTestObject t) throws Exception {
    }

  @Override
  public String toString() {
    return "ExpensiveTestObjectFactoryApache{" + "maxIdleMillis=" + maxIdleMillis
            + ", nrUsesToFailAfter=" + nrUsesToFailAfter + ", minOperationMillis="
            + minOperationMillis + ", maxOperationMillis=" + maxOperationMillis + '}';
  }



}
