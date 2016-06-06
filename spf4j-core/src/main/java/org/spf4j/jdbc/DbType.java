
package org.spf4j.jdbc;

/**
 *
 * @author zoly
 */
public enum DbType {
  ORACLE, H2, SYBASE_ASE, SYBASE_IQ, MSSQL, MYSQL, POSTGRES, COCKROACH_DB;

  /**
   * Default database type to use in all jdbc APIs if a DBtype is not provided.
   */
  public static final DbType DEFAULT = DbType.valueOf(System.getProperty("spf4j.jdbc.defaultDbType", "H2"));

}
