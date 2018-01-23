/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.failsafe;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Throwables;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.test.log.HandlerRegistration;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.log.TestLoggers;

/**
 *
 * @author Zoltan Farkas
 */
public class RetryPolicyTest {

  private static final Logger LOG = LoggerFactory.getLogger(RetryPolicyTest.class);

  @Test(expected = IOException.class)
  public void testNoRetryPolicy() throws IOException, InterruptedException, TimeoutException {
    RetryPolicy<Object, Callable<Object>> rp = RetryPolicy.newBuilder().build((Class) Callable.class);
    rp.execute(() -> {
      throw new IOException();
    }, IOException.class);
  }

  @Test
  public void testNoRetryPolicy2() throws InterruptedException, TimeoutException {
    RetryPolicy<Object, Callable<Object>> rp = RetryPolicy.newBuilder().build((Class) Callable.class);
    String result = (String) rp.execute(() -> "test", RuntimeException.class);
    Assert.assertEquals("test", result);
  }

  @Test
  public void testComplexRetry() throws InterruptedException, TimeoutException, IOException, ExecutionException {
    RetryPolicy<Response, ServerCall> rp = RetryPolicy.<Response, ServerCall>newBuilder()
            .exceptionPredicateBuilder()
            .withPartialPredicate((t, sc)
                    -> Throwables.getIsRetryablePredicate().test(t) ? RetryDecision.retryDefault(sc) : null).finishPredicate()
            .resultPredicateBuilder().withPartialPredicate((resp, sc) -> {
              switch (resp.getType()) {
                case CLIENT_ERROR:
                  return RetryDecision.abort();
                case REDIRECT:
                  return RetryDecision.retry(0, new ServerCall(sc.getServer(),
                          new Request((String) resp.getPayload(), sc.getRequest().getDeadlineMSEpoch())));
                case RETRY_LATER:
                  return RetryDecision.retry(
                          TimeUnit.NANOSECONDS.convert((Long) resp.getPayload() - System.currentTimeMillis(),
                                  TimeUnit.MILLISECONDS), sc);
                case TRANSIENT_ERROR:
                  return RetryDecision.retryDefault(sc);
                case ERROR:
                case OK:
                  return RetryDecision.abort();
                default:
                  throw new IllegalStateException("Unsupported " + resp.getType());
              }
            }).finishPredicate()
            .build(ServerCall.class);
    Server server = new Server();
    try (HandlerRegistration printer = TestLoggers.config().print("", Level.DEBUG)) {
      Response response1 = new Response(Response.Type.OK, "");
      server.setResponse("url1", (r) -> response1);
      server.setResponse("url2", (r) -> new Response(Response.Type.REDIRECT, "url1"));
      long deadlineMillis = System.currentTimeMillis() + 1000;
      ServerCall serverCall = new ServerCall(server, new Request("url2", deadlineMillis));
      long timeoutms = TimeUnit.NANOSECONDS.toMillis(serverCall.getDeadlineNanos() - TimeSource.nanoTime());
      LOG.info("Timeout = {}", timeoutms);
      LogAssert retryExpect = TestLoggers.config().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG,
              Matchers.hasProperty("message",
                      Matchers.startsWith("Result Response{type=REDIRECT, payload=url1} for ServerCall")));
      Response resp
              = rp.execute(serverCall, SocketException.class);
      Assert.assertEquals(response1, resp);
      retryExpect.assertSeen();
      server.breakException(new SocketException("Bla bla"));
      LogAssert retryExpect2 = TestLoggers.config().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG, 3,
              Matchers.hasProperty("message",
                      Matchers.startsWith("Result java.net.SocketException: Bla bla for ServerCall")));
      try {
        rp.execute(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000)), IOException.class);
        Assert.fail();
      } catch (TimeoutException ex) {
        Throwables.writeTo(ex, System.err, Throwables.PackageDetail.SHORT);
        // as expected.
      }
      retryExpect2.assertSeen();
      Future<?> submit = DefaultScheduler.INSTANCE.schedule(() -> server.breakException(null), 100, TimeUnit.MILLISECONDS);
      rp.execute(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000)), IOException.class);
      submit.get();
    }
  }

}
