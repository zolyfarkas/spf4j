
package org.spf4j.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * A process level semaphore implementation based on the JDK semaphore.
 * @author zoly
 */
public final class LocalSemaphore implements Semaphore {

  private final java.util.concurrent.Semaphore semaphore;

  public LocalSemaphore(final int nrPermits, final boolean fair) {
    semaphore = new java.util.concurrent.Semaphore(nrPermits, fair);
  }


  @Override
  public boolean tryAcquire(final int nrPermits, final long timeout, final TimeUnit unit)
          throws InterruptedException {
    return semaphore.tryAcquire(nrPermits, timeout, unit);
  }

  @Override
  public void release(final int nrPermits) {
    semaphore.release(nrPermits);
  }

  @Override
  public String toString() {
    return "LocalSemaphore{" + "semaphore=" + semaphore + '}';
  }

}
