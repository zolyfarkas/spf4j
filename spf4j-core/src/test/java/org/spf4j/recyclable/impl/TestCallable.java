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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.Template;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 *
 * @author zoly
 */
public final class TestCallable implements Callable<Integer> {
    private final RecyclingSupplier<ExpensiveTestObject> pool;
    private final int testNr;

    public TestCallable(final RecyclingSupplier<ExpensiveTestObject> pool, final int testNr) {
        this.pool = pool;
        this.testNr = testNr;
    }

    @Override
    @SuppressFBWarnings("BED_BOGUS_EXCEPTION_DECLARATION")
    public Integer call() throws IOException, InterruptedException {
        Template.doOnSupplied((final ExpensiveTestObject object, final long deadline) -> {
          object.doStuff();
        }, pool, 5, 1000, 60000);
        return testNr;
    }

  @Override
  public String toString() {
    return "TestCallable{" + "pool=" + pool + ", testNr=" + testNr + '}';
  }

}
