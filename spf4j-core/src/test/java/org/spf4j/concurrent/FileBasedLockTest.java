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
package org.spf4j.concurrent;

import com.google.common.io.Files;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({"MDM_THREAD_YIELD", "PATH_TRAVERSAL_IN"})
public final class FileBasedLockTest {

  private static final Logger LOG = LoggerFactory.getLogger(FileBasedLockTest.class);

  private static final String LOCK_FILE = org.spf4j.base.Runtime.TMP_FOLDER + File.separatorChar
          + "test.lock";
  private volatile boolean isOtherRunning;

  @Test(timeout = 60000)
  public void test() throws IOException, InterruptedException, ExecutionException {

    FileBasedLock lock = FileBasedLock.getLock(new File(LOCK_FILE));
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
    LOG.debug("Lock test successful");

  }

  @Test
  @SuppressFBWarnings("AFBR_ABNORMAL_FINALLY_BLOCK_RETURN")
  public void testReentrace() throws IOException {
    File tmp = File.createTempFile("bla", ".lock");
    try (FileBasedLock lock = FileBasedLock.getLock(tmp)) {
      lock.lock();
      try {
        lock.lock();
        try {
          LOG.debug("Reentered lock");
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

  public static void main(final String[] args) throws InterruptedException, IOException {
    FileBasedLock lock = FileBasedLock.getLock(new File(args[0]));
    lock.lock();
    try {
      Thread.sleep(100);
    } finally {
      lock.unlock();
    }
    System.exit(0);
  }

  @Test(expected = OverlappingFileLockException.class)
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testFileLock() throws IOException {
    File tmp = File.createTempFile("test", ".lock");
    tmp.deleteOnExit();
    RandomAccessFile file = new RandomAccessFile(tmp, "rws");
    FileChannel channel = file.getChannel();
    FileLock lock = channel.lock();
    FileLock lock1 = channel.lock();
    Assert.fail();
    lock1.release();
    lock.release();
  }

  @Test
  public void testFileLock2() throws IOException, InterruptedException {
    File tmp = File.createTempFile("test", ".lock");
    tmp.deleteOnExit();
    FileBasedLock lock = FileBasedLock.getLock(tmp);
    Assert.assertTrue(lock.tryLock());
    FileBasedLock lock2 = FileBasedLock.getLock(tmp);
    Assert.assertTrue(lock2.tryLock(1, TimeUnit.SECONDS));
    lock2.unlock();
    lock.unlock();
  }

  @Test
  @SuppressFBWarnings("UAC_UNNECESSARY_API_CONVERSION_FILE_TO_PATH")
  public void testFileLockPermissions() throws IOException {
    File tmpDir = Files.createTempDir();
    Set<PosixFilePermission> reqPermissions = EnumSet.of(PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(reqPermissions);
    File file = new File(tmpDir, "file.lock");
    FileBasedLock lock = FileBasedLock.getLock(file, attr);
    Assert.assertTrue(lock.tryLock());
    lock.unlock();
    Set<PosixFilePermission> actualPermissions = java.nio.file.Files.getPosixFilePermissions(file.toPath());
    Assert.assertEquals(reqPermissions, actualPermissions);
  }

}
