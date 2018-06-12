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
package org.spf4j.unix;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//CHECKSTYLE:OFF
import sun.misc.Signal;
//CHECKSTYLE:ON

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE")
public class CLibraryTest {

  private static final Logger LOG = LoggerFactory.getLogger(CLibraryTest.class);

  @Test
  public void testStrSignal() {
    String strsignal = CLibrary.INSTANCE.strsignal(9);
    Assert.assertThat(strsignal, Matchers.startsWith("Kill"));
  }

  @Test
  public void testSignal() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    CountDownLatch latch = new CountDownLatch(1);
    Signal.handle(new Signal("USR2"), (signal) -> {
      LOG.debug("Received signal: {}", signal);
      latch.countDown();
    });
    org.spf4j.base.Runtime.jrunAndLog(CLibraryTest.class, 60000);
    Assert.assertTrue(latch.await(1, TimeUnit.MINUTES));

  }

  public static void main(final String[] args) {
    int ppid = CLibrary.INSTANCE.getppid();
    Signal signal = new Signal("USR2");
    LOG.info("Sending signal: {}", signal);
    CLibrary.INSTANCE.kill(ppid, signal.getNumber());
  }

}
