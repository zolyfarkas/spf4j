package org.spf4j.concurrent.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.h2.jdbcx.JdbcDataSource;
import org.spf4j.stackmonitor.FastStackCollector;

/**
 *
 * @author zoly
 */
public final class DecentSemaphoreHandler {

  static {
    System.setProperty("spf4j.heartbeat.intervalMillis", "2000"); // 2 second heartbeat
  }

  @SuppressFBWarnings("MDM_THREAD_YIELD")
  public static void main(final String[] args) throws InterruptedException, TimeoutException {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        FastStackCollector.dumpToPrintStream(System.err);
      }

    });
    String connectionString = args[0];
    String semaphoreName = args[1];
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL(connectionString);
    ds.setUser("sa");
    ds.setPassword("sa");
    JdbcSemaphore semaphore = new JdbcSemaphore(ds, semaphoreName, 3);
    for (int i = 0; i < 50; i++) {
      semaphore.acquire(1, 1L, TimeUnit.SECONDS);
      Thread.sleep((long) (Math.random() * 10) + 10);
      System.out.println(System.currentTimeMillis());
      Thread.sleep((long) (Math.random() * 10) + 10);
      semaphore.release();
    }
    semaphore.close();
    System.exit(0);
  }

}
