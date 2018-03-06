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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Throwables;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.DefaultContextAwareExecutor;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.failsafe.concurrent.RetryExecutor;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.log.LogMatchers;
import org.spf4j.test.log.TestLoggers;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
public class RetryPolicyTest {

  private static final Logger LOG = LoggerFactory.getLogger(RetryPolicyTest.class);

  private static RetryExecutor es;

  @BeforeClass
  public static void init() {
    es = new RetryExecutor(DefaultContextAwareExecutor.instance(), null);
  }

  @AfterClass
  public static void shutdown() throws InterruptedException {
    es.close();
  }

  @Test(expected = IOException.class)
  public void testNoRetryPolicy() throws IOException, InterruptedException, TimeoutException {
    RetryPolicy<Object, Callable<Object>> rp = RetryPolicy.<Object, Callable<Object>>newBuilder().build();
    rp.call(() -> {
      throw new IOException();
    }, IOException.class);
  }

  @Test
  public void testNoRetryPolicy2() throws InterruptedException, TimeoutException {
    @SuppressWarnings("unchecked")
    RetryPolicy<Object, Callable<Object>> rp = RetryPolicy.<Object, Callable<Object>>newBuilder().build();
    String result = (String) rp.call(() -> "test", RuntimeException.class);
    Assert.assertEquals("test", result);
  }

  @Test
  public void testComplexRetrySync() throws InterruptedException, TimeoutException, IOException, ExecutionException {
    RetryPolicy<Response, ServerCall> rp = buildRetryPolicy();
    Server server = new Server();
    Response response1 = new Response(Response.Type.OK, "");
    server.setResponse("url1", (r) -> response1);
    server.setResponse("url2", (r) -> new Response(Response.Type.REDIRECT, "url1"));
    server.setResponse("url3", (r) -> new Response(Response.Type.ERROR, "boooo"));
    testSyncRetry(server, rp, response1);
  }

  @Test
  public void testComplexRetryASync() throws InterruptedException, TimeoutException, IOException, ExecutionException {
    RetryPolicy<Response, ServerCall> rp = buildRetryPolicy();
    Server server = new Server();
    Response response1 = new Response(Response.Type.OK, "");
    server.setResponse("url1", (r) -> response1);
    server.setResponse("url2", (r) -> new Response(Response.Type.REDIRECT, "url1"));
    server.setResponse("url3", (r) -> new Response(Response.Type.ERROR, "boooo"));
    testASyncRetry(server, rp, response1);
  }

  public final  RetryPolicy<Response, ServerCall> buildRetryPolicy() {
    return RetryPolicy.<Response, ServerCall>newBuilder()
            .withDeadlineSupplier((c) -> c.getDeadlineNanos())
            .retryPredicateBuilder()
            .withExceptionPartialPredicate((t, sc)
                    -> Throwables.getIsRetryablePredicate().test(t) ? RetryDecision.retryDefault(sc) : null)
            .withResultPartialPredicate((resp, sc) -> {
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
                  return null;
                case OK:
                  return RetryDecision.abort();
                default:
                  throw new IllegalStateException("Unsupported " + resp.getType());
              }
            }).withResultPartialPredicate((resp, sc)
            -> (resp.getType() == Response.Type.ERROR)
            ? RetryDecision.retryDefault(sc)
            : RetryDecision.abort(), 3)
            .finishPredicate()
            .withExecutorService(es)
            .build();
  }

  public final void testSyncRetry(final Server server, final RetryPolicy<Response, ServerCall> rp,
          final Response response1)
          throws InterruptedException, TimeoutException, ExecutionException, IOException {
    long deadlineMillis = System.currentTimeMillis() + 1000;
    ServerCall serverCall = new ServerCall(server, new Request("url2", deadlineMillis));
    long timeoutms = TimeUnit.NANOSECONDS.toMillis(serverCall.getDeadlineNanos() - TimeSource.nanoTime());
    LOG.info("Timeout = {}", timeoutms);
    LogAssert retryExpect = TestLoggers.sys().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG,
            LogMatchers.hasMatchingMessage(
                    Matchers.startsWith("Result Response{type=REDIRECT, payload=url1} for ServerCall")));
    Response resp
            = rp.call(serverCall, SocketException.class);
    Assert.assertEquals(response1, resp);
    retryExpect.assertObservation();
    server.breakException(new SocketException("Bla bla"));
    LogAssert retryExpect2 = TestLoggers.sys().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG, 3,
            LogMatchers.hasMatchingMessage(
                    Matchers.startsWith("Result java.net.SocketException: Bla bla for ServerCall")));
    try {
      rp.call(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000)), IOException.class);
      Assert.fail();
    } catch (TimeoutException ex) {
      LOG.debug("Expected exception", ex);
      // as expected.
    }
    retryExpect2.assertObservation();
    Future<?> submit = DefaultScheduler.INSTANCE.schedule(
            () -> server.breakException(null), 100, TimeUnit.MILLISECONDS);
    rp.call(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000)), IOException.class);
    submit.get();
    // Test error response
    LogAssert retryExpect3 = TestLoggers.sys().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "^Result Response[{]type=ERROR, payload=boooo[}] for ServerCall.+ retrying .+$"));
    Response er = rp.call(new ServerCall(server,
            new Request("url3", System.currentTimeMillis() + 1000)), IOException.class);
    Assert.assertEquals("boooo", er.getPayload());
    retryExpect3.assertObservation();
    // Test error response the second type, to make sure predicate state is handled correctly
    LogAssert retryExpect4 = TestLoggers.sys().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "^Result Response[{]type=ERROR, payload=boooo[}] for ServerCall.+ retrying .+$"));
    Response er2 = rp.call(new ServerCall(server,
            new Request("url3", System.currentTimeMillis() + 1000)), IOException.class);
    Assert.assertEquals("boooo", er2.getPayload());
    retryExpect4.assertObservation();
  }

  public final void testASyncRetry(final Server server, final RetryPolicy<Response, ServerCall> rp,
          final Response response1)
          throws InterruptedException, TimeoutException, ExecutionException, IOException {
    long deadlineMillis = System.currentTimeMillis() + 1000;
    ServerCall serverCall = new ServerCall(server, new Request("url2", deadlineMillis));
    long timeoutms = TimeUnit.NANOSECONDS.toMillis(serverCall.getDeadlineNanos() - TimeSource.nanoTime());
    LOG.info("Timeout = {}", timeoutms);
    LogAssert retryExpect = TestLoggers.sys().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG,
            LogMatchers.hasMatchingMessage(
                    Matchers.startsWith("Result Response{type=REDIRECT, payload=url1} for ServerCall")));
    Response resp = rp.submit(serverCall).get();
    Assert.assertEquals(response1, resp);
    retryExpect.assertObservation();
    server.breakException(new SocketException("Bla bla"));
    LogAssert retryExpect2 = TestLoggers.sys().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG, 3,
            LogMatchers.hasMatchingMessage(
                    Matchers.startsWith("Result java.net.SocketException: Bla bla for ServerCall")));
    try {
      rp.submit(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000))).get();
      Assert.fail();
    } catch (ExecutionException ex) {
      LOG.debug("Expected exception", ex);
      Assert.assertEquals(TimeoutException.class, ex.getCause().getClass());
      // as expected.
    }
    retryExpect2.assertObservation();
    Future<?> submit = DefaultScheduler.INSTANCE.schedule(
            () -> server.breakException(null), 100, TimeUnit.MILLISECONDS);
    rp.submit(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000))).get();
    submit.get();
    // Test error response
    LogAssert retryExpect3 = TestLoggers.sys().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "^Result Response[{]type=ERROR, payload=boooo[}] for ServerCall.+ retrying .+$"));
    Response er = rp.submit(new ServerCall(server,
            new Request("url3", System.currentTimeMillis() + 1000))).get();
    Assert.assertEquals("boooo", er.getPayload());
    retryExpect3.assertObservation();
    // Test error response the second type, to make sure predicate state is handled correctly
    LogAssert retryExpect4 = TestLoggers.sys().expect("org.spf4j.failsafe.RetryPredicate", Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "^Result Response[{]type=ERROR, payload=boooo[}] for ServerCall.+ retrying .+$"));
    Response er2 = rp.submit(new ServerCall(server,
            new Request("url3", System.currentTimeMillis() + 1000))).get();
    Assert.assertEquals("boooo", er2.getPayload());
    retryExpect4.assertObservation();
  }

}
