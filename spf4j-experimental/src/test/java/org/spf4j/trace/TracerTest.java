
package org.spf4j.trace;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spf4j.base.Throwables;
import org.spf4j.concurrent.DefaultExecutor;

/**
 *
 * @author zoly
 */
public class TracerTest {


  private static Tracer TRACER;

  @Test
  public void testTrace() throws InterruptedException {
    try (TraceScope trace = TRACER.continueOrNewTrace("myTraceId", -1, "testTraceSpan")) {
     doSomething(1);
    }
  }

  @Test
  public void testDistTrace() throws InterruptedException {
    try (TraceScope trace = TRACER.continueOrNewTrace("clientTraceId", -1, "clientInvokeSpan")) {
      REQ_QUEUE.put(new Message("test msg", ImmutableMap.of("trace-id", trace.getTraceId(),
              "span-id", trace.getCurrentSpanId())));
      Message<String> response = RESP_QUEUE.take();
      System.out.println("Response = " + response);
    }
  }


  public void doSomething(final int index) throws InterruptedException {
    try (SpanScope sb = TRACER.getTraceScope().startSpan("doSomething")) {
      sb.log("arg0", index);
      Thread.sleep((long) (Math.random() * 1000));
    }
  }

  public int getSomething(final int index) throws InterruptedException {
    final TraceScope ts = TRACER.getTraceScope();
    return ts.executeSpan("ts1", (SpanScope span) -> {
      span.log("arg0", index);
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
            Map<String, String> headers = take.getHeaders();
            String traceId = headers.get("trace-id");
            int spanId;
            String spanIdStr = headers.get("span-id");
            if (spanIdStr == null) {
              spanId = -1;
            } else {
              spanId = Integer.parseInt(headers.get("span-id"));
            }
            TraceScope traceScope = TRACER.continueOrNewTrace(traceId, spanId, "serverHandler");
            try (SpanScope scope = traceScope.getCurrentSpan()) {
              Callable<Void> tracedCallable = traceScope.getTracedCallable(() -> {
                doSomething(100);
                return null;
              });
              Future<Void> submit = DefaultExecutor.INSTANCE.submit(tracedCallable);
              RESP_QUEUE.put(new Message("RESP" + take.getPayload() + getSomething(1) + submit.get(),
                      Collections.EMPTY_MAP));
            }
          }
        } catch (InterruptedException ex) {
          //terminate this worker
          Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
          Throwables.writeTo(ex, System.err, Throwables.Detail.STANDARD);
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
