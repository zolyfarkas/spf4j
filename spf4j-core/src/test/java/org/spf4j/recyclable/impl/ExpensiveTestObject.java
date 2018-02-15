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
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a pooled object implementation, that behaves like a connection object.
 *
 * @author zoly
 */
@SuppressFBWarnings("PREDICTABLE_RANDOM") //not security related
public final class ExpensiveTestObject implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(ExpensiveTestObject.class);

  private static final AtomicInteger OBJ_COUNT = new AtomicInteger();

  private static volatile boolean failAll = false;

  private final long maxIdleMillis;
  private final int nrUsesToFailAfter;
  private final long minOperationMillis;
  private final long maxOperationMillis;
  private long lastTouchedTimeMillis;
  private int nrUses;
  private final String id;

  public static boolean isFAILALL() {
    return failAll;
  }

  public static void setFailAll(final boolean failAll) {
    ExpensiveTestObject.failAll = failAll;
  }

  @SuppressFBWarnings("STT_TOSTRING_STORED_IN_FIELD")
  public ExpensiveTestObject(final long maxIdleMillis, final int nrUsesToFailAfter,
          final long minOperationMillis, final long maxOperationMillis) {
    this.maxIdleMillis = maxIdleMillis;
    this.nrUsesToFailAfter = nrUsesToFailAfter;
    this.minOperationMillis = minOperationMillis;
    this.maxOperationMillis = maxOperationMillis;
    lastTouchedTimeMillis = System.currentTimeMillis();
    nrUses = 0;
    simulateDoStuff(maxOperationMillis - minOperationMillis);
    id = "Test Object " + OBJ_COUNT.getAndIncrement();
  }

  public void doStuff() throws IOException {
    if (failAll) {
      throw new IOExceptionImpl("Failall " + id);
    }
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastTouchedTimeMillis > maxIdleMillis) {
      throw new ClosedChannelException();
    }
    if (nrUses > nrUsesToFailAfter) {
      throw new IOExceptionImpl("Simulated random crap " + id);
    }
    simulateDoStuff(maxOperationMillis - minOperationMillis);
    nrUses++;
    lastTouchedTimeMillis = System.currentTimeMillis();
  }

  public void testObject() throws IOException {
    LOG.debug("Testing object {}", id);
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastTouchedTimeMillis > maxIdleMillis) {
      throw new IOException("Connection closed " + id);
    }
    if (nrUses > nrUsesToFailAfter) {
      throw new IOExceptionImpl("Simulated random crap " + id);
    }
    simulateDoStuff(0);
    nrUses++;
    lastTouchedTimeMillis = System.currentTimeMillis();
  }

  @Override
  public void close() throws IOException {
    doStuff();
  }

  @SuppressFBWarnings("MDM_THREAD_YIELD")
  private void simulateDoStuff(final long time) {
    long sleepTime = (long) (Math.random() * (time));
    try {
      Thread.sleep(minOperationMillis + sleepTime);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public String toString() {
    return "ExpensiveTestObject{" + "maxIdleMillis=" + maxIdleMillis + ", nrUsesToFailAfter="
            + nrUsesToFailAfter + ", minOperationMillis=" + minOperationMillis + ", maxOperationMillis="
            + maxOperationMillis + ", lastTouchedTimeMillis=" + lastTouchedTimeMillis + ", nrUses=" + nrUses
            + ", id=" + id + '}';
  }

  private static final class IOExceptionImpl extends IOException {

    IOExceptionImpl(final String message) {
      super(message);
    }

    @Override
    public Throwable fillInStackTrace() {
      return this;
    }
  }

}
