
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


  /**
   * Return the SQL for a current time millis since a EPOCH...
   *
   * @param dbType - the database type.
   * @return - the sql fragment taht returns the current sql millis.
   * @throws ExceptionInInitializerError
   */
  public static String getCurrTSSqlFn(final DbType dbType) throws ExceptionInInitializerError {
    switch (dbType) {
      case H2:
        return "TIMESTAMPDIFF('MILLISECOND', timestamp '1970-01-01 00:00:00', CURRENT_TIMESTAMP())";
      case ORACLE:
        return "(SYSDATE - TO_DATE('01-01-1970 00:00:00', 'DD-MM-YYYY HH24:MI:SS')) * 24 * 3600000";
      case MSSQL:
        return "DATEDIFF(ms, '1970-01-01 00:00:00', GETUTCDATE())";
      case POSTGRES:
        return "extract(epoch FROM now()) * 1000";
      case COCKROACH_DB:
        return "extract(epoch_nanosecond from now()) / 1e6";
      default:
        throw new IllegalArgumentException("Database not supported " + dbType);
    }
  }

  /**
   * Return the SQL for a current time millis since a EPOCH...
   *
   * @param dbType - the database type.
   * @return - the sql fragment taht returns the current sql millis.
   * @throws ExceptionInInitializerError
   */
  public String getCurrTSSqlFn() throws ExceptionInInitializerError {
    return getCurrTSSqlFn(this);
  }


}
