package org.spf4j.concurrent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Special purpose queue for a single value
 *
 * @author zoly
 */
public final class UnitQueueP<T> {

    private final AtomicReference<T> value = new AtomicReference<>();

    private final Object wsync = new Object();

    public T poll() {
        T result = value.getAndSet(null);
        if (result != null) {
            return result;
        } else {
            return null;
        }
    }

    private static final Semaphore SPIN_LIMIT = new Semaphore(org.spf4j.base.Runtime.NR_PROCESSORS - 1);

    public T poll(final long timeoutNanos, final long spinCount) throws InterruptedException {
        T result = poll();
        if (result != null) {
            return result;
        }
        if (org.spf4j.base.Runtime.NR_PROCESSORS > 1 && spinCount > 0) {
            boolean tryAcquire = SPIN_LIMIT.tryAcquire();
            if (tryAcquire) {
                try {
                    int i = 0;
                    int j = 64;
                    while (i < spinCount) {
                        if (i % j == 0) {
                            result = poll();
                            if (result != null) {
                                return result;
                            }
                            if (j > 4) {
                                j = j / 2;
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
        synchronized (wsync) {
            while ((result = value.getAndSet(null)) == null) {
                final long to = deadlineNanos - System.nanoTime();
                if (to <= 0) {
                    return null;
                }
                wsync.wait(TimeUnit.MILLISECONDS.convert(to, TimeUnit.NANOSECONDS));
            }
        }
        return result;
    }

    public boolean offer(final T offer) {
        boolean result = value.compareAndSet(null, offer);
        if (result) {
            synchronized (wsync) {
                wsync.notifyAll();
            }
        }
        return result;
    }

}
