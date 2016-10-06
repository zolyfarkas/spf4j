package org.spf4j.concurrent.jdbc;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.joda.time.DateTime;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class JdbcSemaphoreTest {

  private static String hbddl;
  private static String semddl;

  static {
    try {
      hbddl = Resources.toString(Resources.getResource("heartBeats.sql"), Charsets.US_ASCII);
      semddl = Resources.toString(Resources.getResource("semaphoreTable.sql"), Charsets.US_ASCII);
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  static void createSchemaObjects(DataSource ds) throws SQLException {
    try (Connection conn = ds.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute(hbddl);
      }

      try (Statement stmt = conn.createStatement()) {
        stmt.execute(semddl);
      }
    }
  }


  @Test
  public void testSingleProcess() throws SQLException, IOException, InterruptedException, TimeoutException {

    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");
    try (Connection conn = ds.getConnection()) { // only to keep the schema arround in thsi section
      createSchemaObjects(ds);

      JdbcHeartBeat heartbeat = JdbcHeartBeat.getHeartBeatAndSubscribe(ds,
              HeartBeatTableDesc.DEFAULT, (JdbcHeartBeat.LifecycleHook) null);
      long lb = heartbeat.getLastRunDB();
      System.out.println("last TS = " + lb + " " + new DateTime(lb));
      heartbeat.beat();

      testReleaseAck(ds, "testSem", 2);
      testReleaseAck(ds, "testSem2", 2);
      heartbeat.close();
    }

  }

  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testReleaseAck(final DataSource ds, final String semName, final int maxReservations)
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
  public void testMultiProcess() throws SQLException, IOException, InterruptedException, ExecutionException, TimeoutException {
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
  public void testMultiProcess2() throws SQLException, IOException, InterruptedException, ExecutionException, TimeoutException {
    Server server = Server.createTcpServer(new String[]{"-tcpPort", "9123", "-tcpAllowOthers"}).start();
    try  {
    File tempDB = File.createTempFile("test", "h2db");
    tempDB.deleteOnExit();
    String connStr = "jdbc:h2:tcp://localhost:9123/nio:" + tempDB.getAbsolutePath() + ";AUTO_SERVER=TRUE";
      JdbcDataSource ds = new JdbcDataSource();
      ds.setURL(connStr);
      ds.setUser("sa");
      ds.setPassword("sa");
      createSchemaObjects(ds);
      JdbcSemaphore semaphore = new JdbcSemaphore(ds, "test_sem2", 1, true);
      String o1 = org.spf4j.base.Runtime.jrun(DecentSemaphoreHandler.class, 10000, connStr, "test_sem2");
      String o2 = org.spf4j.base.Runtime.jrun(DecentSemaphoreHandler.class, 10000, connStr, "test_sem2");
      Assert.assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));
      Assert.assertFalse(semaphore.tryAcquire(10, TimeUnit.SECONDS));
      System.out.println("P1:");
      System.out.println(o1);
      System.out.println("P2:");
      System.out.println(o2);
      String [] nr1 = o1.split("\n");
      String [] nr2 = o2.split("\n");
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

}
