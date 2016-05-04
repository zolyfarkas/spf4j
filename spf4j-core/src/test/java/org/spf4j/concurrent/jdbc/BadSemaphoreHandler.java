package org.spf4j.concurrent.jdbc;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.h2.jdbcx.JdbcDataSource;

/**
 *
 * @author zoly
 */
public class BadSemaphoreHandler {

  static {
    System.setProperty("spf4j.heartbeat.intervalMillis", "2000"); // 2 second heartbeat
  }

  public static void main(final String[] args) throws SQLException, InterruptedException, TimeoutException {

    String connectionString = args[0];
    String semaphoreName = args[1];
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL(connectionString);
    ds.setUser("sa");
    ds.setPassword("sa");
    JdbcSemaphore semaphore = new JdbcSemaphore(ds, semaphoreName, 3);
    semaphore.acquire(1, TimeUnit.SECONDS, 1);
    System.exit(0);
  }

}
