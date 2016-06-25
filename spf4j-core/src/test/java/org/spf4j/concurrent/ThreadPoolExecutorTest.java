
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

/**
 * test to validate the behavior between spf4j and FJP implementations...
 * @author zoly
 */
public class ThreadPoolExecutorTest {


  @Test(timeout = 10000)
  public void testInteruptionBehavior() throws InterruptedException, ExecutionException {
    LifoThreadPoolExecutorSQP executor = new LifoThreadPoolExecutorSQP("test", 0, 16, 60000, 0, 1024);
    testPoolTaskCancellation(executor);
  }

  @Test(timeout = 10000)
  public void testInteruptionBehaviorFJP() throws InterruptedException, ExecutionException {
    boolean assertError = false;
    try {
      testPoolTaskCancellation(new ForkJoinPool(16));
    } catch (AssertionError err) {
      System.err.println("expected " + err);
      assertError = true;
    }
    Assert.assertTrue("expected that FJP tasks cannot be interrupted", assertError);
  }

  public void testPoolTaskCancellation(ExecutorService executor) throws InterruptedException, ExecutionException {
    RunnableImpl testRunnable = new RunnableImpl();
    Future<?> submit = executor.submit(testRunnable);
    Assert.assertTrue("task did not start", testRunnable.getStartedlatch().await(5, TimeUnit.SECONDS));
    submit.cancel(true);
    try {
      submit.get();
      Assert.fail("expected CancellationException");
    } catch (CancellationException ex) {
      // expected
    }
    Assert.assertTrue("task was not interrupted", testRunnable.getInterruptedLatch().await(5, TimeUnit.SECONDS));
    executor.shutdown();
    Assert.assertTrue("executor was not shut down", executor.awaitTermination(1000, TimeUnit.MILLISECONDS));
  }

  static class RunnableImpl implements Runnable {

    private CountDownLatch startedlatch = new CountDownLatch(1);

    private CountDownLatch interruptedLatch = new CountDownLatch(1);

    @Override
    @SuppressFBWarnings("MDM_THREAD_YIELD")
    public void run() {
      startedlatch.countDown();
      try {
        Thread.sleep(100000);
      } catch (InterruptedException ex) {
        interruptedLatch.countDown();
      }
    }

    public CountDownLatch getInterruptedLatch() {
      return interruptedLatch;
    }




    public CountDownLatch getStartedlatch() {
      return startedlatch;
    }




  }


}
