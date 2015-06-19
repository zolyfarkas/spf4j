
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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Special purpose queue for a single value
 * Custom designed for the LifoThreadPool
 *
 * @author zoly
 */
public final class UnitQueuePU<T> {

    private final AtomicReference<T> value = new AtomicReference<>();

    private final Thread readerThread;

    public UnitQueuePU(final Thread readerThread) {
        this.readerThread = readerThread;
    }



    public T poll() {
        T result = value.getAndSet(null);
        if (result != null) {
            return result;
        } else {
            return null;
        }
    }

    private static final int SPIN_LIMITER = Integer.getInteger("lifoTp.spinLimiter", 1);

    private static final Semaphore SPIN_LIMIT = new Semaphore(org.spf4j.base.Runtime.NR_PROCESSORS - SPIN_LIMITER);

    public T poll(final long timeoutNanos, final long spinCount) throws InterruptedException {
        T result = poll();
        if (result != null) {
            return result;
        }
        if (spinCount > 0 && org.spf4j.base.Runtime.NR_PROCESSORS > 1) {
            boolean tryAcquire = SPIN_LIMIT.tryAcquire();
            if (tryAcquire) {
                try {
                    int i = 0;
                    while (i < spinCount) {
                        if (i % 4 == 0) {
                            result = poll();
                            if (result != null) {
                                return result;
                            }
                        }
                        i++;
                    }
                } finally {
                    SPIN_LIMIT.release();
                }
            }
        }

            long deadlineNanos = System.nanoTime() + timeoutNanos;
                while ((result = value.getAndSet(null)) == null) {
                    final long to = deadlineNanos - System.nanoTime();
                    if (to <= 0) {
                        return null;
                    }
                    LockSupport.parkNanos(to);
                }
        return result;
    }

    public boolean offer(final T offer) {
        boolean result = value.compareAndSet(null, offer);
        if (result) {
            LockSupport.unpark(readerThread);
        }
        return result;
    }

    @Override
    public String toString() {
        return "UnitQueuePU{" + "value=" + value + '}';
    }

}
