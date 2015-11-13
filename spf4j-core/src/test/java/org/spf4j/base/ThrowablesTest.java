/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.base;

import java.net.SocketTimeoutException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class ThrowablesTest {

    /**
     * Test of chain method, of class ExceptionChain.
     */
    @Test
    public void testChain() {
        System.out.println("chain");
        Throwable t = new RuntimeException("", new SocketTimeoutException("Boo timeout"));
        Throwable newRootCause = new TimeoutException("Booo");
        Throwable result = Throwables.chain(t, newRootCause);
        System.out.println(Throwables.toString(result));
        Assert.assertEquals(newRootCause, com.google.common.base.Throwables.getRootCause(result));
        Assert.assertEquals(3, com.google.common.base.Throwables.getCausalChain(result).size());

    }

    @Test
    public void testChain2() {
        System.out.println("chain");
        Throwable t = new RuntimeException("bla1",
                new BatchUpdateException("Sql bla", "ORA-500", 500, new int[] {1, 2}, new RuntimeException("la la")));
        Throwable newRootCause = new TimeoutException("Booo");
        Throwable result = Throwables.chain(t, newRootCause);
        System.out.println(Throwables.toString(result));
        Assert.assertArrayEquals(new int[] {1, 2}, ((BatchUpdateException) result.getCause()).getUpdateCounts());
        Assert.assertEquals(newRootCause, com.google.common.base.Throwables.getRootCause(result));
        Assert.assertEquals(4, com.google.common.base.Throwables.getCausalChain(result).size());

    }

    @Test
    public void testChain3() {
        Exception e = new RuntimeException();
        for (int i = 0; i < 10; i++) {
            e = Throwables.suppress(e, new RuntimeException());
        }
        Throwable [] suppressed = Throwables.getSuppressed(e);
        Assert.assertEquals(10, suppressed.length);
        final SQLException sqlException = new SQLException(e);
        sqlException.setNextException(new SQLException("bla"));
        System.out.println(Throwables.toString(sqlException));

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
            exs = Throwables.suppress(new Exception("test"  + i), exs);
        }
        Assert.assertEquals(200, Throwables.getNrRecursiveSuppressedExceptions(exs));

    }


}
