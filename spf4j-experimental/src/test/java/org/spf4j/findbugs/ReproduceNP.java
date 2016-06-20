
package org.spf4j.findbugs;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

/**
 *
 * @author zoly
 */
public class ReproduceNP {

    private DataSource getDataSource() {
      return new JdbcDataSource();
    }

    private List<String> doSomething(final Connection conn, final String something) {
      System.out.println("conn" + conn);
      List<String> result =  new ArrayList<>();
      result.add(something);
      return result;
    }


    public final List<String> testNPFP(final String something) throws SQLException {
      try (Connection conn = getDataSource().getConnection()) {
        return doSomething(conn, something);
      }
    }

}
