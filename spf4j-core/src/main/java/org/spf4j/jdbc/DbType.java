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
