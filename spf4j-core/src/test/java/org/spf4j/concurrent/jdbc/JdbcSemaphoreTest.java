package org.spf4j.concurrent.jdbc;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class JdbcSemaphoreTest {

  private static String hbddl;
  private static String semddl;

  @BeforeClass
  public static void init() throws IOException {
    hbddl = Resources.toString(Resources.getResource("heartBeats.sql"), Charsets.US_ASCII);
    semddl = Resources.toString(Resources.getResource("semaphoreTable.sql"), Charsets.US_ASCII);
  }

  @Test
  public void testSingleProcess() throws SQLException, IOException, InterruptedException {

    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");

    try (Connection conn = ds.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute(hbddl);
      }

      try (Statement stmt = conn.createStatement()) {
        stmt.execute(semddl);
      }

      testReleaseAck(ds, "testSem", 2);
      testReleaseAck(ds, "testSem2", 2);
    }

  }

  public void testReleaseAck(final JdbcDataSource ds, final String semName, final int maxReservations)
          throws SQLException, InterruptedException {
    JdbcSemaphore semaphore = new JdbcSemaphore(ds, semName, maxReservations);
    Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS, 1));
    semaphore.release(1);
    Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS, 2));
    Assert.assertFalse(semaphore.tryAcquire(2, TimeUnit.SECONDS, 1));
    semaphore.release(1);
    semaphore.release(1);
    Assert.assertEquals(maxReservations, semaphore.availablePermits());
    Assert.assertEquals(maxReservations, semaphore.totalPermits());
    semaphore.reducePermits(1);
    Assert.assertEquals(maxReservations - 1, semaphore.totalPermits());
    Assert.assertEquals(maxReservations - 1, semaphore.availablePermits());
    Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS, 1));
    Assert.assertFalse(semaphore.tryAcquire(2, TimeUnit.SECONDS, 1));
    semaphore.release(1);
    try {
      semaphore.release(1);
      Assert.fail("should not be allow to release!");
    } catch (IllegalStateException ex) {
      Assert.assertTrue(ex.getMessage().contains("Trying to release more than you own"));
    }
    semaphore.increasePermits(1);
    Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS, 2));
    semaphore.reducePermits(1);
    semaphore.release(2);
    Assert.assertFalse(semaphore.tryAcquire(10, TimeUnit.SECONDS, 2));

  }

  @Test
  public void testMultiProcess() throws SQLException, IOException, InterruptedException, ExecutionException, TimeoutException {
    Server server = Server.createTcpServer(new String[]{"-tcpPort", "9123", "-tcpAllowOthers"}).start();

    File tempDB = File.createTempFile("test", "h2db");
    String connStr = "jdbc:h2:tcp://localhost:9123/nio:" + tempDB.getAbsolutePath() + ";AUTO_SERVER=TRUE";
    try {
      JdbcDataSource ds = new JdbcDataSource();
      ds.setURL(connStr);
      ds.setUser("sa");
      ds.setPassword("sa");

      try (Connection conn = ds.getConnection()) {
        try (Statement stmt = conn.createStatement()) {
          stmt.execute(hbddl);
        }

        try (Statement stmt = conn.createStatement()) {
          stmt.execute(semddl);
        }
      }
      testReleaseAck(ds, "testSem", 2);
      JdbcSemaphore semaphore = new JdbcSemaphore(ds, "test_sem2", 3);
      org.spf4j.base.Runtime.jrun(BadSemaphoreHandler.class, 10000, connStr, "test_sem2");
      org.spf4j.base.Runtime.jrun(BadSemaphoreHandler.class, 10000, connStr, "test_sem2");
      Assert.assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS, 1));
      Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS, 1));

      server.stop();
    } finally {
      tempDB.delete();
    }
  }

}
