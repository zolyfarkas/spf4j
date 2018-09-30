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

import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.junit.Assert;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.TimeSource;
import org.spf4j.pool.jdbc.PooledDataSource;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({"HARD_CODE_PASSWORD", "SQL_INJECTION_JDBC"})
public class JdbcSemaphoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(JdbcSemaphoreTest.class);

  private static String hbddl;
  private static String semddl;

  static {
    try {
      hbddl = Resources.toString(Resources.getResource("heartBeats.sql"), StandardCharsets.US_ASCII);
      semddl = Resources.toString(Resources.getResource("semaphoreTable.sql"), StandardCharsets.US_ASCII);
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  static void createSchemaObjects(final DataSource ds) throws SQLException {
    try (Connection conn = ds.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute(hbddl);
      }

      try (Statement stmt = conn.createStatement()) {
        stmt.execute(semddl);
      }
    }
  }

  static PooledDataSource createPooledDS(final JdbcDataSource ds) throws ObjectCreationException {

    return new PooledDataSource(0, 4, new RecyclingSupplier.Factory<Connection>() {
      @Override
      public Connection create() throws ObjectCreationException {
        try {
          return ds.getConnection();
        } catch (SQLException ex) {
          throw new ObjectCreationException(ex);
        }
      }

      @Override
      public void dispose(final Connection object) throws ObjectDisposeException {
        try {
          object.close();
        } catch (SQLException ex) {
          throw new ObjectDisposeException(ex);
        }
      }

      @Override
      public boolean validate(final Connection object, final Exception e) throws SQLException {
        return object.isValid(60);
      }
    });

  }

  @Test
  public void testSingleProcess() throws SQLException, IOException, InterruptedException, TimeoutException,
          ObjectCreationException, ObjectDisposeException {

    JdbcDataSource hds = new JdbcDataSource();
    hds.setURL("jdbc:h2:mem:test");
    hds.setUser("sa");
    hds.setPassword("sa");
    PooledDataSource ds = createPooledDS(hds);
    try (Connection conn = ds.getConnection()) { // only to keep the schema arround in thsi section
      createSchemaObjects(ds);

      JdbcHeartBeat heartbeat = JdbcHeartBeat.getHeartBeatAndSubscribe(ds,
              HeartBeatTableDesc.DEFAULT, (JdbcHeartBeat.LifecycleHook) null);
      long lb = heartbeat.getLastRunDB();
      LOG.debug("last TS = {}", Instant.ofEpochMilli(lb));
      heartbeat.beat();

      testReleaseAck(ds, "testSem", 2);
      testReleaseAck(ds, "testSem2", 2);
      heartbeat.close();
    }
    ds.close();

  }

  @Test
  public void testSingleProcessLock() throws SQLException, IOException, InterruptedException, TimeoutException {

    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");
    try (Connection conn = ds.getConnection()) { // only to keep the schema arround in thsi section
      createSchemaObjects(ds);
      JdbcLock lock = new JdbcLock(ds, SemaphoreTablesDesc.DEFAULT, "testLock", 10);
      lock.lock();
      Assert.assertFalse(lock.tryLock());
      lock.unlock();
    }

  }

  @Test(expected = SQLException.class)
  public void testSingleMultipleInstance() throws SQLException, IOException, InterruptedException, TimeoutException {

    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");
    try (Connection conn = ds.getConnection()) { // only to keep the schema arround in this section
      createSchemaObjects(ds);
      JdbcLock lock = new JdbcLock(ds, SemaphoreTablesDesc.DEFAULT, "testLock", 10);
      JdbcLock lock2 = new JdbcLock(ds, SemaphoreTablesDesc.DEFAULT, "testLock", 10);
      lock.lock();
      Assert.assertFalse(lock.tryLock());
      lock.unlock();
      lock2.lock();
      lock.unlock();
    }

  }

  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public static void testReleaseAck(final DataSource ds, final String semName, final int maxReservations)
          throws SQLException, InterruptedException, TimeoutException {
    JdbcSemaphore semaphore = new JdbcSemaphore(ds, semName, maxReservations);

    // test update;
    int totalPermits = semaphore.totalPermits();
    int acquire = totalPermits - 1;
    semaphore.acquire(acquire, 1, TimeUnit.SECONDS);
    Assert.assertEquals(1, semaphore.permitsOwned());
    int nrPermits = semaphore.totalPermits();
    semaphore.updatePermits(nrPermits + 1);
    Assert.assertEquals(nrPermits + 1, semaphore.totalPermits());
    semaphore.updatePermits(nrPermits);
    Assert.assertEquals(nrPermits, semaphore.totalPermits());
    semaphore.reducePermits(2);
    Assert.assertFalse(semaphore.tryAcquire(2, TimeUnit.SECONDS));
    semaphore.release(acquire);
    semaphore.increasePermits(2);
    Assert.assertEquals(totalPermits, semaphore.totalPermits());

    Assert.assertTrue(semaphore.tryAcquire(1, 10, TimeUnit.SECONDS));
    semaphore.release(1);
    Assert.assertTrue(semaphore.tryAcquire(2, 10, TimeUnit.SECONDS));
    Assert.assertFalse(semaphore.tryAcquire(1, 2, TimeUnit.SECONDS));
    semaphore.release(1);
    semaphore.release(1);
    Assert.assertEquals(maxReservations, semaphore.availablePermits());
    Assert.assertEquals(maxReservations, semaphore.totalPermits());
    semaphore.reducePermits(1);
    Assert.assertEquals(maxReservations - 1, semaphore.totalPermits());
    Assert.assertEquals(maxReservations - 1, semaphore.availablePermits());
    Assert.assertTrue(semaphore.tryAcquire(1, 10, TimeUnit.SECONDS));
    Assert.assertFalse(semaphore.tryAcquire(1, 2, TimeUnit.SECONDS));
    semaphore.release(1);
    try {
      semaphore.release(1);
      Assert.fail("should not be allow to release!");
    } catch (IllegalStateException ex) {
      Assert.assertTrue(ex.getMessage().contains("Trying to release more than you own"));
    }
    semaphore.increasePermits(1);
    Assert.assertTrue(semaphore.tryAcquire(2, 10, TimeUnit.SECONDS));
    semaphore.reducePermits(1);
    semaphore.release(2);
    Assert.assertFalse(semaphore.tryAcquire(2, 10, TimeUnit.SECONDS));

  }

  @Test
  @SuppressFBWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
  public void testMultiProcess()
          throws SQLException, IOException, InterruptedException, ExecutionException, TimeoutException {
    Server server = Server.createTcpServer(new String[]{"-tcpPort", "9123", "-tcpAllowOthers"}).start();

    File tempDB = File.createTempFile("test", "h2db");
    String connStr = "jdbc:h2:tcp://localhost:9123/nio:" + tempDB.getAbsolutePath() + ";AUTO_SERVER=TRUE";
    try {
      JdbcDataSource ds = new JdbcDataSource();
      ds.setURL(connStr);
      ds.setUser("sa");
      ds.setPassword("sa");

      createSchemaObjects(ds);
      testReleaseAck(ds, "testSem", 2);
      JdbcSemaphore semaphore = new JdbcSemaphore(ds, "test_sem2", 3);
      org.spf4j.base.Runtime.jrun(BadSemaphoreHandler.class, 10000, connStr, "test_sem2");
      org.spf4j.base.Runtime.jrun(BadSemaphoreHandler.class, 10000, connStr, "test_sem2");
      Assert.assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));
      Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));
      JdbcHeartBeat.stopHeartBeats();
      server.stop();
    } finally {
      if (!tempDB.delete()) {
        throw new IOException("Cannot delete " + tempDB);
      }
    }
  }

  @Test

  public void testMultiProcess2()
          throws SQLException, IOException, InterruptedException, ExecutionException, TimeoutException {
    Server server = Server.createTcpServer(new String[]{"-tcpPort", "9123", "-tcpAllowOthers"}).start();
    try {
      File tempDB = File.createTempFile("test", "h2db");
      tempDB.deleteOnExit();
      String connStr = "jdbc:h2:tcp://localhost:9123/nio:" + tempDB.getAbsolutePath() + ";AUTO_SERVER=TRUE";
      JdbcDataSource ds = new JdbcDataSource();
      ds.setURL(connStr);
      ds.setUser("sa");
      ds.setPassword("sa");
      createSchemaObjects(ds);
      JdbcSemaphore semaphore = new JdbcSemaphore(ds, "test_sem2", 1, true);
      String o1 = org.spf4j.base.Runtime.jrun(DecentSemaphoreHandler.class, 10000000, connStr, "test_sem2").toString();
      String o2 = org.spf4j.base.Runtime.jrun(DecentSemaphoreHandler.class, 10000000, connStr, "test_sem2").toString();
      Assert.assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));
      Assert.assertFalse(semaphore.tryAcquire(10, TimeUnit.SECONDS));
      LOG.debug("P1: {}", o1);
      LOG.debug("P2: {}", o2);
      String[] nr1 = o1.split("\n");
      String[] nr2 = o2.split("\n");
      int totatl = nr1.length + nr2.length;
      Set<String> numbers = new HashSet<>(totatl);
      numbers.addAll(Arrays.asList(nr1));
      numbers.addAll(Arrays.asList(nr2));
      Assert.assertEquals(totatl, numbers.size());
    } finally {
      JdbcHeartBeat.stopHeartBeats();
      server.stop();
    }
  }

  @Test
  @Ignore
  public void testPerformance()
          throws SQLException, IOException, InterruptedException, ExecutionException, TimeoutException {
    Server server = Server.createTcpServer(new String[]{"-tcpPort", "9123", "-tcpAllowOthers"}).start();
    try {
      File tempDB = File.createTempFile("test", "h2db");
      tempDB.deleteOnExit();
      String connStr = "jdbc:h2:tcp://localhost:9123/nio:" + tempDB.getAbsolutePath() + ";AUTO_SERVER=TRUE";
      JdbcDataSource ds = new JdbcDataSource();
      ds.setURL(connStr);
      ds.setUser("sa");
      ds.setPassword("sa");
      createSchemaObjects(ds);
      JdbcSemaphore semaphore = new JdbcSemaphore(ds, "test_sem2", 1, true);
      Sampler s = new Sampler(5, 5000);
      s.registerJmx();
      s.start();
      LOG.info("started sampling");
      long deadline = TimeSource.nanoTime() + TimeUnit.SECONDS.toNanos(10);
      do {
        semaphore.acquire(1, 1, TimeUnit.SECONDS);
        semaphore.release();
      } while (deadline > TimeSource.nanoTime());
      semaphore.close();
      s.stop();
      LOG.debug("dumped samples to {}", s.dumpToFile());
      } finally {
        JdbcHeartBeat.stopHeartBeats();
        server.stop();
      }
  }


}
