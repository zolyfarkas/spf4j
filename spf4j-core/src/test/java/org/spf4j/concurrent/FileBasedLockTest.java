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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public final class FileBasedLockTest {

    private static final String LOCK_FILE = org.spf4j.base.Runtime.TMP_FOLDER + File.separatorChar
            + "test.lock";
    private volatile boolean isOtherRunning;

    @Test(timeout = 60000)
    public void test() throws IOException, InterruptedException, ExecutionException {

        FileBasedLock lock = new FileBasedLock(new File(LOCK_FILE));
        lock.lock();
        Future<Void> future = DefaultExecutor.INSTANCE.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                org.spf4j.base.Runtime.jrun(FileBasedLockTest.class, 60000, LOCK_FILE);
                isOtherRunning = false;
                return null;
            }
        });

        try {
            isOtherRunning = true;
            for (int i = 1; i < 10000; i++) {
                if (!isOtherRunning) {
                    future.get();
                    Assert.fail("The Other process should be running, lock is not working");
                }
                Thread.sleep(1);
            }
        } finally {
            lock.unlock();
        }
        System.out.println("Lock test successful");

    }

    @Test
    @SuppressFBWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
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
              boolean tryLock = lock.tryLock();
              Assert.assertTrue(tryLock);
              lock.unlock();
            } finally {
                lock.unlock();
            }
        } finally {
          if (!tmp.delete()) {
            throw new IOException("Cannot delete " + tmp);
          }
        }
    }


    public static void main(final String[] args) throws  InterruptedException, IOException {
        FileBasedLock lock = new FileBasedLock(new File(args[0]));
        lock.lock();
        try {
            Thread.sleep(100);
        } finally {
            lock.unlock();
        }
        System.exit(0);
    }
}