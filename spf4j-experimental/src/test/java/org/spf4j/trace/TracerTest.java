
package org.spf4j.trace;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author zoly
 */
public class TracerTest {


  private static Tracer TRACER;

  @Test
  public void testTrace() throws InterruptedException {
    try (TraceScope trace = TRACER.continueOrNewTrace("myTrace", null)) {
     doSomething(1);
    }
  }

  @Test
  public void testDistTrace() throws InterruptedException {
    try (TraceScope trace = TRACER.continueOrNewTrace("clientTrace", null)) {
      REQ_QUEUE.put(new Message("test msg", ImmutableMap.of("trace-id", "")));
    }
  }


  public void doSomething(final int index) throws InterruptedException {
    try (TraceScope span = TRACER.getTraceScope().startSpan("something-" + index)) {
      Thread.sleep((long) (Math.random() * 1000));
    }
  }

  public int getSomething(final int index) throws InterruptedException {
    final TraceScope ts = TRACER.getTraceScope();
    return ts.executeSpan("ts1", (TraceScope span) -> {
      span.log(LogLevel.DEBUG, "input is = {}", index);
      long sleep = (long) (Math.random() * 1000);
      if (sleep < 10) {
        throw new RuntimeException("simulated error " + index);
      }
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException ex) {
        // do nothing;
      }
      return index * 10;
    });
  }


  private volatile boolean isRunning = false;

  @Before
  public void startServer() {
    isRunning = true;
    for (int i = 0; i < 4; i++) {
      DefaultExecutor.INSTANCE.submit(() -> {
        try {
          while (isRunning) {
            Message<String> take = REQ_QUEUE.take();
            String traceId = take.getHeaders().get("trace-id");
            try (TraceScope scope = TRACER.continueOrNewTrace("svcHandle", traceId)) {
              RESP_QUEUE.put(new Message("RESP" + take.getPayload() + getSomething(1), Collections.EMPTY_MAP));
            }
          }
        } catch (InterruptedException ex) {
          //terminate this worker
          Thread.currentThread().interrupt();
        }
        System.out.println("Finishing server worker");
      });
    }
  }

  @After
  public void stopServer() throws InterruptedException {
    isRunning = false;
  }


  private final BlockingQueue<Message<String>> REQ_QUEUE = new LinkedBlockingDeque<>();
  private final BlockingQueue<Message<String>> RESP_QUEUE = new LinkedBlockingDeque<>();




}
