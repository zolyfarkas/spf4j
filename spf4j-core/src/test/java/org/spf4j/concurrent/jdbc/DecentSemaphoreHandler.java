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
package org.spf4j.concurrent.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({"PREDICTABLE_RANDOM", "HARD_CODE_PASSWORD" })
public final class DecentSemaphoreHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DecentSemaphoreHandler.class);

  static {
    System.setProperty("spf4j.heartbeat.intervalMillis", "2000"); // 2 second heartbeat
  }

  private DecentSemaphoreHandler() { }

  @SuppressFBWarnings("MDM_THREAD_YIELD")
  public static void main(final String[] args)
          throws InterruptedException, TimeoutException, SQLException, IOException {
    String connectionString = args[0];
    String semaphoreName = args[1];
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL(connectionString);
    ds.setUser("sa");
    ds.setPassword("sa");
    Sampler s = new Sampler(5, 10000);
    s.registerJmx();
    s.start();
    LOG.info("started sampling");
    JdbcSemaphore semaphore = new JdbcSemaphore(ds, semaphoreName, 3);
    for (int i = 0; i < 50; i++) {
      semaphore.acquire(1, 1L, TimeUnit.SECONDS);
      Thread.sleep((long) (Math.random() * 10) + 10);
      LOG.info("beat");
      Thread.sleep((long) (Math.random() * 10) + 10);
      semaphore.release();
    }
    semaphore.close();
    File dumpToFile = s.dumpToFile();
    LOG.info("stack samples dumped to {}", dumpToFile);
    s.stop();
    System.exit(0);
  }

}
