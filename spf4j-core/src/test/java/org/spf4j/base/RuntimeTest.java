
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

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author zoly
 */
public final class RuntimeTest {

    public RuntimeTest() {
    }

    /**
     * Test of goDownWithError method, of class Runtime.
     */
    @Test
    public void testSomeParams() throws IOException, InterruptedException, ExecutionException {
        System.out.println("PID=" + Runtime.PID);
        System.out.println("OSNAME=" + Runtime.OS_NAME);
        System.out.println("NR_OPEN_FILES=" + Runtime.getNrOpenFiles());
        System.out.println("LSOF_OUT=" + Runtime.getLsofOutput());
        System.out.println("MAX_OPEN_FILES=" + Runtime.Ulimit.MAX_NR_OPENFILES);
    }

    @Test(expected = ExecutionException.class)
    public void testExitCode() throws IOException, InterruptedException, ExecutionException {
        Runtime.jrun(RuntimeTest.TestError.class, 60000);
    }

    @Test(expected = ExecutionException.class)
    public void testExitCode2() throws IOException, InterruptedException, ExecutionException {
        Runtime.jrun(RuntimeTest.TestError2.class, 60000);
    }

    public static final class TestError {

        public static void main(final String [] args) {
            throw new RuntimeException();
        }
    }

    public static final class TestError2 {

        public static void main(final String [] args) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    e.printStackTrace();
                }
            });
            throw new RuntimeException();
        }
    }



}