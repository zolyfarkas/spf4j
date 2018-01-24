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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public final class ThrowablesTest {

  private static final Logger LOG = LoggerFactory.getLogger(ThrowablesTest.class);

  /**
   * Test of chain method, of class ExceptionChain.
   */
  @Test(timeout = 3000)
  public void testChain() {
    Throwable t = new RuntimeException("", new SocketTimeoutException("Boo timeout"));
    Throwable newRootCause = new TimeoutException("Booo");
    Throwable result = Throwables.chain(t, newRootCause);
    LOG.debug("Thowable string = {}", Throwables.toString(result));
    Assert.assertEquals(newRootCause, com.google.common.base.Throwables.getRootCause(result));
    Assert.assertEquals(3, com.google.common.base.Throwables.getCausalChain(result).size());
    Throwable firstCause = Throwables.firstCause(t, (l) -> false);
    Assert.assertNull(firstCause);
  }

  @Test
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
  public void testChain2() {
    Throwable t = new RuntimeException("bla1",
            new BatchUpdateException("Sql bla", "ORA-500", 500, new int[]{1, 2}, new RuntimeException("la la")));
    Throwable newRootCause = new TimeoutException("Booo");
    Throwable result = Throwables.chain(t, newRootCause);
    LOG.debug("Thowable string = {}", Throwables.toString(result));
    Assert.assertArrayEquals(new int[]{1, 2}, ((BatchUpdateException) result.getCause()).getUpdateCounts());
    Assert.assertEquals(newRootCause, com.google.common.base.Throwables.getRootCause(result));
    Assert.assertEquals(4, com.google.common.base.Throwables.getCausalChain(result).size());

  }

  @Test
  public void testChain3() {
    Exception e = new RuntimeException(new RuntimeException(new RuntimeException()));
    for (int i = 0; i < 10; i++) {
      e = Throwables.suppress(e, new RuntimeException());
    }
    Throwable[] suppressed = Throwables.getSuppressed(e);
    Assert.assertEquals(10, suppressed.length);
    final SQLException sqlException = new SQLException(e);
    sqlException.setNextException(new SQLException("bla", new RuntimeException(new RuntimeException())));
    sqlException.setNextException(new SQLException("bla"));
    LOG.debug("Thowable string = {}", Throwables.toString(sqlException));
    Assert.assertEquals(2, Throwables.getSuppressed(sqlException).length);

  }

  @Test
  public void testSuppressedManipulation() {
    Exception ex = new Exception("test");
    Assert.assertEquals(0, Throwables.getNrSuppressedExceptions(ex));
    final Exception sex = new Exception("tests");
    ex.addSuppressed(sex);
    Assert.assertEquals(1, Throwables.getNrSuppressedExceptions(ex));
    sex.addSuppressed(new Exception("tests2"));
    Assert.assertEquals(1, Throwables.getNrSuppressedExceptions(ex));
    Assert.assertEquals(2, Throwables.getNrRecursiveSuppressedExceptions(ex));
    Throwables.removeOldestSuppressedRecursive(ex);
    Assert.assertEquals(1, Throwables.getNrRecursiveSuppressedExceptions(ex));

    Exception exs = new Exception(ex);
    for (int i = 0; i < 500; i++) {
      exs = Throwables.suppress(new Exception("test" + i), exs);
    }
    Assert.assertEquals(100, Throwables.getNrRecursiveSuppressedExceptions(exs));
  }

  @Test
  public void testRecoverable() {
    Throwable t = new RuntimeException();
    Assert.assertFalse(Throwables.containsNonRecoverable(t));
    t.addSuppressed(new IOException());
    Assert.assertFalse(Throwables.containsNonRecoverable(t));
    t.addSuppressed(new OutOfMemoryError());
    Assert.assertTrue(Throwables.containsNonRecoverable(t));
    t = new RuntimeException(new RuntimeException(new IOException("Too many open files")));
    Assert.assertTrue(Throwables.containsNonRecoverable(t));
    t = new RuntimeException(new RuntimeException(new StackOverflowError()));
    Assert.assertFalse(Throwables.containsNonRecoverable(t));
  }

  @Test
  public void testAbbreviation() throws IOException {
    StringBuilder sb = new StringBuilder();
    Throwables.writeAbreviatedClassName("org.spf4j.Class", sb);
    Assert.assertEquals("o.s.Class", sb.toString());

    sb = new StringBuilder();
    Throwables.writeAbreviatedClassName("Class", sb);
    Assert.assertEquals("Class", sb.toString());

  }

}
