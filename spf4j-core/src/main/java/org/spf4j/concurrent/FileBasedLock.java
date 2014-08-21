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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File based Lock implementation, that can be used as IPC method.
 *
 * @author zoly
 */
public final class FileBasedLock implements Lock, java.io.Closeable {

    public static final Map<String, Lock> JVM_LOCKS = new HashMap<String, Lock>();
    private final RandomAccessFile file;
    private final Lock jvmLock;
    private FileLock fileLock;

    public FileBasedLock(final File lockFile) throws FileNotFoundException {
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
        try {
            fileLock = file.getChannel().lock();
            writeHolderInfo();
        } catch (IOException ex) {
            jvmLock.unlock();
            throw new RuntimeException(ex);
        } catch (RuntimeException ex) {
            jvmLock.unlock();
            throw ex;
        }
    }

    @Override
    @SuppressFBWarnings({"MDM_WAIT_WITHOUT_TIMEOUT", "EXS_EXCEPTION_SOFTENING_HAS_CHECKED", "MDM_THREAD_YIELD" })
    public void lockInterruptibly() throws InterruptedException {
        jvmLock.lockInterruptibly();
        try {
            final FileChannel channel = file.getChannel();
            //CHECKSTYLE:OFF
            while ((fileLock = channel.tryLock()) == null && !Thread.interrupted()) {
                //CHECKSTYLE:ON
                Thread.sleep(1);
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            writeHolderInfo();
        } catch (InterruptedException ex) {
            jvmLock.unlock();
            throw ex;
        } catch (IOException ex) {
            jvmLock.unlock();
            throw new RuntimeException(ex);
        } catch (RuntimeException ex) {
            jvmLock.unlock();
            throw ex;
        }
    }

    @Override
    @SuppressFBWarnings("MDM_THREAD_FAIRNESS")
    public boolean tryLock() {
        if (jvmLock.tryLock()) {
            try {
                fileLock = file.getChannel().tryLock();
                if (fileLock != null) {
                    writeHolderInfo();
                    return true;
                } else {
                    return false;
                }
            } catch (IOException ex) {
                jvmLock.unlock();
                throw new RuntimeException(ex);
            } catch (RuntimeException ex) {
                jvmLock.unlock();
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
            try {
                long waitTime = 0;
                long maxWaitTime = unit.toMillis(time);
                while (waitTime < maxWaitTime
                        //CHECKSTYLE:OFF
                        && (fileLock = file.getChannel().tryLock()) != null
                        //CHECKSTYLE:ON
                        && !Thread.interrupted()) {
                    Thread.sleep(1);
                    waitTime++;
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if (fileLock != null) {
                    writeHolderInfo();
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException ex) {
                jvmLock.unlock();
                throw ex;
            } catch (IOException ex) {
                jvmLock.unlock();
                throw new RuntimeException(ex);
            } catch (RuntimeException ex) {
                jvmLock.unlock();
                throw ex;
            }
        } else {
            return false;
        }
    }

    @Override
    public void unlock() {
        try {
            fileLock.release();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        jvmLock.unlock();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            close();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            file.close();
        } finally {
            jvmLock.unlock();
        }
    }

    private void writeHolderInfo() throws IOException {
        file.seek(0);
        byte [] data = org.spf4j.base.Runtime.PROCESS_NAME.getBytes(Charsets.UTF_8);
        file.write(data);
        file.setLength(data.length);
    }
}
