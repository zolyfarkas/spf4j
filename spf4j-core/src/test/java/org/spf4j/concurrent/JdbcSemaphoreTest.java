
package org.spf4j.concurrent;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class JdbcSemaphoreTest {
  
  @Test
  public void testSomeMethod() throws SQLException, IOException, InterruptedException {
    
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test");
    ds.setUser("sa");
    ds.setPassword("sa");
    
    String ddl = Resources.toString(Resources.getResource("lockTable.sql"), Charsets.US_ASCII);
    
    try (Connection conn = ds.getConnection()) {
      Statement stmt = conn.createStatement();
      stmt.execute(ddl);
      
      JdbcSemaphore semaphore = new JdbcSemaphore(ds);
      Assert.assertTrue(semaphore.acquire(10, TimeUnit.SECONDS));
      semaphore.release();  
      
      Assert.assertTrue(semaphore.acquire(10, TimeUnit.SECONDS));
      Assert.assertTrue(semaphore.acquire(10, TimeUnit.SECONDS));
      Assert.assertFalse(semaphore.acquire(10, TimeUnit.SECONDS));
      semaphore.release();
      semaphore.release();
    }
    
    
  }
  
}
