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
package org.spf4j.concurrent;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import junit.framework.Assert;
import org.junit.Test;
import org.spf4j.base.AbstractRunnable;

/**
 *
 * @author zoly
 */
public final class FileBasedLockTest {

    private static final String LOCK_FILE = org.spf4j.base.Runtime.TMP_FOLDER + File.separatorChar
            + "test.lock";
    private volatile boolean isOtherRunning;

    @Test(timeout = 60000)
    public void test() throws IOException, InterruptedException {

        final String classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
        final String jvmPath =
                System.getProperties().getProperty("java.home")
                + File.separatorChar + "bin" + File.separatorChar + "java";


        Thread extProcThread = new Thread(new AbstractRunnable() {
            @Override
            public void doRun() throws IOException, InterruptedException {
                String[] command = new String[]{jvmPath, "-cp", classPath, FileBasedLockTest.class.getName()};
                System.out.println("Running " + Arrays.toString(command));
                Process proc = Runtime.getRuntime().exec(command);
                int result = proc.waitFor();
                isOtherRunning = !(result == 0);
            }
        });
        FileBasedLock lock = new FileBasedLock(new File(LOCK_FILE));
        lock.lock();
        try {
            extProcThread.start();
            isOtherRunning = true;
            for (int i = 1; i < 10000; i++) {
                if (!isOtherRunning) {
                    Assert.fail("The Other process should be running, lock is not working");
                }
                Thread.sleep(1);
            }
        } finally {
            lock.unlock();
        }
        extProcThread.join();
        System.out.println("Lock test successful");

    }

    @Test
    public void testReentrace() throws IOException {
        File tmp = File.createTempFile("bla", ".lock");
        try (FileBasedLock lock = new FileBasedLock(tmp)) {
            lock.lock();
            try {
                lock.lock();
                try {
                    System.out.println("Reentered lock");
                } finally {
                    lock.unlock();
                }
            } finally {
                lock.unlock();
            }
        }
    }


    public static void main(final String[] args) throws  InterruptedException, IOException {
        FileBasedLock lock = new FileBasedLock(new File(LOCK_FILE));
        lock.lock();
        try {
            Thread.sleep(100);
        } finally {
            lock.unlock();
        }
        System.exit(0);
    }
}