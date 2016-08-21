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

import com.google.common.base.Charsets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.WillClose;

/**
 * File based Lock implementation, that can be used as IPC method.
 *
 * @author zoly
 */
public final class FileBasedLock implements Lock, java.io.Closeable {

    private static final Map<String, Lock> JVM_LOCKS = new HashMap<>();
    private final RandomAccessFile file;
    private final Lock jvmLock;
    private FileLock fileLock;
    private volatile Thread owner;
    private int reentranceCount = 0;

    private static int next(final int maxVal) {
        return Math.abs(Math.abs(ThreadLocalRandom.current().nextInt()) % maxVal);
    }

    public FileBasedLock(final File lockFile) throws IOException {
        file = new RandomAccessFile(lockFile, "rws");
        String filePath = lockFile.getPath();
        synchronized (JVM_LOCKS) {
            Lock lock = JVM_LOCKS.get(filePath);
            if (lock == null) {
                lock = new ReentrantLock();
                JVM_LOCKS.put(filePath, lock);
            }
            jvmLock = lock;
        }
        fileLock = null;
    }

    @Override
    @SuppressFBWarnings("MDM_WAIT_WITHOUT_TIMEOUT")
    public void lock() {
        jvmLock.lock();
        final Thread currentThread = Thread.currentThread();
        if (currentThread.equals(owner)) {
                // already own the lock.
                reentranceCount++;
                return;
        }
        try {
            fileLock = file.getChannel().lock();
            owner = currentThread;
            reentranceCount++;
            writeHolderInfo();
        } catch (IOException ex) {
            unlockInternal();
            throw new RuntimeException(ex);
        } catch (RuntimeException ex) {
            unlockInternal();
            throw ex;
        }
    }

    private void unlockInternal() {
        jvmLock.unlock();
        reentranceCount--;
        if (reentranceCount == 0) {
            owner = null;
        } else if (reentranceCount < 0) {
            throw new RuntimeException("Not owner of this lock " + this);
        }
    }

    @Override
    @SuppressFBWarnings({"MDM_WAIT_WITHOUT_TIMEOUT", "EXS_EXCEPTION_SOFTENING_HAS_CHECKED", "MDM_THREAD_YIELD" })
    public void lockInterruptibly() throws InterruptedException {
        jvmLock.lockInterruptibly();
        final Thread currentThread = Thread.currentThread();
        if (currentThread.equals(owner)) {
                // already own the lock.
                reentranceCount++;
                return;
        }
        try {
            final FileChannel channel = file.getChannel();
            //CHECKSTYLE:OFF
            while ((fileLock = channel.tryLock()) == null && !Thread.interrupted()) {
                //CHECKSTYLE:ON
                Thread.sleep(next(1000));
            }
            owner = currentThread;
            reentranceCount++;
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            writeHolderInfo();
        } catch (InterruptedException | RuntimeException ex) {
            unlockInternal();
            throw ex;
        } catch (IOException ex) {
            unlockInternal();
            throw new RuntimeException(ex);
        }
    }

    @Override
    @SuppressFBWarnings("MDM_THREAD_FAIRNESS")
    public boolean tryLock() {
        if (jvmLock.tryLock()) {
            final Thread currentThread = Thread.currentThread();
            if (currentThread.equals(owner)) {
                    // already own the lock.
                    reentranceCount++;
                    return true;
            }
            try {
                fileLock = file.getChannel().tryLock();
                if (fileLock != null) {
                    owner = currentThread;
                    reentranceCount++;
                    writeHolderInfo();
                    return true;
                } else {
                    return false;
                }
            } catch (IOException ex) {
                unlockInternal();
                throw new RuntimeException(ex);
            } catch (RuntimeException ex) {
                unlockInternal();
                throw ex;
            }
        } else {
            return false;
        }
    }

    @SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_HAS_CHECKED", "MDM_THREAD_YIELD" })
    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        if (jvmLock.tryLock(time, unit)) {
            final Thread currentThread = Thread.currentThread();
            if (currentThread.equals(owner)) {
                    // already own the lock.
                    reentranceCount++;
                    return true;
            }
            try {
                long waitTime = 0;
                long maxWaitTime = unit.toMillis(time);
                while (waitTime < maxWaitTime
                        //CHECKSTYLE:OFF
                        && (fileLock = file.getChannel().tryLock()) == null
                        //CHECKSTYLE:ON
                        && !Thread.interrupted()) {
                    Thread.sleep(next(1000));
                    waitTime++;
                }
                owner = currentThread;
                reentranceCount++;
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if (fileLock != null) {
                    writeHolderInfo();
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException | RuntimeException ex) {
                unlockInternal();
                throw ex;
            } catch (IOException ex) {
                unlockInternal();
                throw new RuntimeException(ex);
            }
        } else {
            return false;
        }
    }

    @Override
    public void unlock() {
        if (!Thread.currentThread().equals(owner)) {
            throw new IllegalStateException("Lock " + this + " not owned by current thread " + Thread.currentThread());
        }
        try {
            fileLock.release();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            unlockInternal();
        }
    }

    @Override
    public Condition newCondition() {
        return jvmLock.newCondition();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } catch (Throwable t) {
            try {
                forceClose();
            } catch (IOException ex) {
                ex.addSuppressed(t);
                throw ex;
            }
        }
        forceClose();
    }

    @Override
    @WillClose
    public void close() throws IOException {
        Thread o = owner;
        if (o == null) {
            file.close();
        } else if (Thread.currentThread().equals(o)) {
            try {
                file.close();
            } finally {
                unlockInternal();
            }
        } else {
            throw new IllegalStateException("Lock " + this + " not owned by current thread " + Thread.currentThread());
        }
    }

    @WillClose
    public void forceClose() throws IOException {
        file.close();
    }

    private void writeHolderInfo() throws IOException {
        file.seek(0);
        byte[] data = getContextInfo().getBytes(Charsets.UTF_8);
        file.write(data);
        file.setLength(data.length);
        file.getChannel().force(true);
    }

    public static String getContextInfo() {
      return org.spf4j.base.Runtime.PROCESS_ID + ':' + Thread.currentThread().getName();
    }


    @Override
    public String toString() {
        return "FileBasedLock{" + "file=" + file + ", owner=" + owner + ", reentranceCount=" + reentranceCount + '}';
    }


}
