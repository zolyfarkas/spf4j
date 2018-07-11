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
import java.util.concurrent.CancellationException;
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
import org.spf4j.concurrent.DefaultContextAwareExecutor;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.failsafe.concurrent.RetryExecutor;
import org.spf4j.test.log.Level;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.matchers.LogMatchers;
import org.spf4j.test.log.LogRecord;
import org.spf4j.test.log.TestLoggers;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
public class RetryPolicyTest {

  public static final String PREDICATE_CLASS = DefaultRetryPredicate.class.getName();

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

  @Test
  public void testNoRetryPolicy() throws IOException, InterruptedException, TimeoutException {
    LogAssert vex =  TestLoggers.sys().dontExpect(PREDICATE_CLASS, Level.DEBUG, Matchers.any(LogRecord.class));
    try (LogAssert expect = vex) {
      RetryPolicy.noRetryPolicy().run(() -> {
        throw new IOException();
      }, IOException.class);
      Assert.fail();
    } catch (IOException ex) {
      // expected
      vex.assertObservation();
    }
  }


  @Test
  public void testNoRetryPolicyAsync() throws IOException, InterruptedException, TimeoutException {
    LogAssert vex =  TestLoggers.sys().dontExpect(PREDICATE_CLASS, Level.DEBUG, Matchers.any(LogRecord.class));
    try (LogAssert expect = vex) {
      RetryPolicy.newBuilder().buildAsync().submit(() -> {
        throw new IOException();
      }).get();
      Assert.fail();
    } catch (ExecutionException ex) {
      Assert.assertEquals(IOException.class, ex.getCause().getClass());
      // expected
      vex.assertObservation();
    }
  }

  @Test
  public void testDefaulPolicy() throws IOException, InterruptedException, TimeoutException {
    try (LogAssert expect = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG, 2,
            LogMatchers.hasMessageWithPattern(
                    "Result java.lang.RuntimeException.* for org.spf4j.failsafe.RetryPolicyTest.*"))) {
      try {
        RetryPolicy.defaultPolicy().run(() -> {
          throw new RuntimeException();
        }, RuntimeException.class);
        Assert.fail();
      } catch (RuntimeException ex) {
        expect.assertObservation();
      }
    }
  }

  @Test
  public void testDefaulPolicyAsync() throws IOException, InterruptedException, TimeoutException {
    try (LogAssert expect = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG, 2,
            LogMatchers.hasMatchingMessage(
                    Matchers.startsWith("Result java.lang.RuntimeException for org.spf4j.failsafe.RetryPolicyTest")))) {
      try {
        RetryPolicy.newBuilder()
              .withDefaultThrowableRetryPredicate()
              .withRetryOnException(Exception.class, 2) // will retry any other exception twice.
              .buildAsync().submit(() -> {
          throw new RuntimeException();
        }).get();
        Assert.fail();
      } catch (ExecutionException ex) {
        Assert.assertEquals(RuntimeException.class, ex.getCause().getClass());
        expect.assertObservation();
      }
    }
  }


  @Test
  public void testDefaulPolicyInterruption() throws IOException, InterruptedException, TimeoutException {
    try {
      Thread t = Thread.currentThread();
      DefaultScheduler.INSTANCE.schedule(() -> t.interrupt(), 1, TimeUnit.SECONDS);
      RetryPolicy.newBuilder()
            .withRetryOnException(Exception.class, Integer.MAX_VALUE)
            .build().run(() -> {
        throw new IOException();
      }, IOException.class);
      Assert.fail();
    } catch (InterruptedException ex) {
      // expected
    }
  }

  @Test
  public void testDefaulPolicyInterruptionAsync() throws IOException, InterruptedException,
          TimeoutException, ExecutionException {
    Future<Object> res = RetryPolicy.newBuilder()
            .withRetryOnException(Exception.class, Integer.MAX_VALUE)
            .buildAsync().submit(() -> {
      throw new IOException();
    });
    try {
      res.get(100, TimeUnit.MILLISECONDS);
      Assert.fail();
    } catch (TimeoutException tex) {
      // expected to time out.
    }
    res.cancel(true);
    Assert.assertTrue(res.isDone());
    try {
      res.get(100, TimeUnit.MILLISECONDS);
      Assert.fail();
    } catch (CancellationException ex) {
      Assert.assertThat(ex.getSuppressed(), Matchers.arrayWithSize(1));
    }
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
    AsyncRetryExecutor<Response, ServerCall> rp = buildRetryExecutor();
    Server server = new Server();
    Response response1 = new Response(Response.Type.OK, "");
    server.setResponse("url1", (x) -> response1);
    server.setResponse("url2", (r) -> new Response(Response.Type.REDIRECT, "url1"));
    server.setResponse("url3", (r) -> new Response(Response.Type.ERROR, "boooo"));
    assertSyncRetry(server, rp, response1);
  }

  @Test
  public void testComplexRetryASync() throws InterruptedException, TimeoutException, IOException, ExecutionException {
    AsyncRetryExecutor<Response, ServerCall> rp = buildRetryExecutor();
    Server server = new Server();
    Response response1 = new Response(Response.Type.OK, "");
    server.setResponse("url1", (x) -> response1);
    server.setResponse("url2", (r) -> new Response(Response.Type.REDIRECT, "url1"));
    server.setResponse("url3", (r) -> new Response(Response.Type.ERROR, "boooo"));
    assertASyncRetry(server, rp, response1);
  }

  public final  AsyncRetryExecutor<Response, ServerCall> buildRetryExecutor() {
    return RetryPolicy.<Response, ServerCall>newBuilder()
            .withDefaultThrowableRetryPredicate(Integer.MAX_VALUE)
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
            .buildAsync(es);
  }

  @SuppressFBWarnings("CC_CYCLOMATIC_COMPLEXITY")
  public final void assertSyncRetry(final Server server, final AsyncRetryExecutor<Response, ServerCall> rp,
          final Response response1)
          throws InterruptedException, TimeoutException, ExecutionException, IOException {
    long deadlineMillis = System.currentTimeMillis() + 1000;
    ServerCall serverCall = new ServerCall(server, new Request("url2", deadlineMillis));
    long timeoutms = deadlineMillis - System.currentTimeMillis();
    LOG.info("Timeout = {}", timeoutms);
    try (LogAssert retryExpect = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG,
            LogMatchers.hasMatchingMessage(
                    Matchers.startsWith("Result Response{type=REDIRECT, payload=url1} for ServerCall")))) {
      Response resp
              = rp.call(serverCall, SocketException.class, timeoutms, TimeUnit.MILLISECONDS);
      Assert.assertEquals(response1, resp);
      retryExpect.assertObservation();
    }
    server.breakException(new SocketException("Bla bla"));
    try (LogAssert retryExpect2 = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "Result java.net.SocketException for ServerCall.*"))) {
      try {
        rp.run(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000)), IOException.class,
                1000, TimeUnit.MILLISECONDS);
        Assert.fail();
      } catch (TimeoutException ex) {
        LOG.debug("Expected exception", ex);
        // as expected.
      }
      retryExpect2.assertObservation();
    }
    Future<?> submit = DefaultScheduler.INSTANCE.schedule(
            () -> server.breakException(null), 100, TimeUnit.MILLISECONDS);
    Response resp = rp.call(new ServerCall(server,
            new Request("url1", System.currentTimeMillis() + 1000)), IOException.class,
            1000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(Response.Type.OK, resp.getType());
    submit.get();
    // Test error response
    try (LogAssert retryExpect3 = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "^Result Response[{]type=ERROR, payload=boooo[}] for ServerCall.+ retrying .+$"))) {
      Response er = rp.call(new ServerCall(server,
              new Request("url3", System.currentTimeMillis() + 1000)), IOException.class,
              1000, TimeUnit.MILLISECONDS);
      Assert.assertEquals("boooo", er.getPayload());
      retryExpect3.assertObservation();
    }
    // Test error response the second type, to make sure predicate state is handled correctly
    try (LogAssert retryExpect4 = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                      "^Result Response[{]type=ERROR, payload=boooo[}] for ServerCall.+ retrying .+$"))) {
      Response er2 = rp.call(new ServerCall(server,
              new Request("url3", System.currentTimeMillis() + 1000)), IOException.class, 1000, TimeUnit.MILLISECONDS);
      Assert.assertEquals("boooo", er2.getPayload());
      retryExpect4.assertObservation();
    }
  }

  @SuppressFBWarnings("CC_CYCLOMATIC_COMPLEXITY")
  public final void assertASyncRetry(final Server server, final AsyncRetryExecutor<Response, ServerCall> rp,
          final Response response1)
          throws InterruptedException, TimeoutException, ExecutionException, IOException {
    long deadlineMillis = System.currentTimeMillis() + 1000;
    Request request = new Request("url2", deadlineMillis);
    ServerCall serverCall = new ServerCall(server, request);
    long timeoutms = request.getDeadlineMSEpoch() - System.currentTimeMillis();
    LOG.info("Timeout = {}", timeoutms);
    try (LogAssert retryExpect = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG,
            LogMatchers.hasMatchingMessage(
                    Matchers.startsWith("Result Response{type=REDIRECT, payload=url1} for ServerCall")))) {
      Response resp = rp.submit(serverCall, timeoutms, TimeUnit.MILLISECONDS).get();
      Assert.assertEquals(response1, resp);
      retryExpect.assertObservation();
    }
    server.breakException(new SocketException("Bla bla"));
    try (LogAssert retryExpect2 = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "Result java.net.SocketException for ServerCall.*"))) {
      try {
        rp.submit(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000)),
                1000, TimeUnit.MILLISECONDS).get();
        Assert.fail();
      } catch (ExecutionException ex) {
        LOG.debug("Expected exception", ex);
        Assert.assertEquals(TimeoutException.class, ex.getCause().getClass());
        // as expected.
      }
      retryExpect2.assertObservation();
    }
    Future<?> submit = DefaultScheduler.INSTANCE.schedule(
            () -> server.breakException(null), 100, TimeUnit.MILLISECONDS);
    rp.submit(new ServerCall(server, new Request("url1", System.currentTimeMillis() + 1000))).get();
    submit.get();
    // Test error response
    try (LogAssert retryExpect3 = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "^Result Response[{]type=ERROR, payload=boooo[}] for ServerCall.+ retrying .+$"))) {
      Response er = rp.submit(new ServerCall(server,
              new Request("url3", System.currentTimeMillis() + 1000)), 1000, TimeUnit.MILLISECONDS).get();
      Assert.assertEquals("boooo", er.getPayload());
      retryExpect3.assertObservation();
    }
    // Test error response the second type, to make sure predicate state is handled correctly
    try (LogAssert retryExpect4 = TestLoggers.sys().expect(PREDICATE_CLASS, Level.DEBUG, 3,
            LogMatchers.hasMessageWithPattern(
                    "^Result Response[{]type=ERROR, payload=boooo[}] for ServerCall.+ retrying .+$"))) {
      Response er2 = rp.submit(new ServerCall(server,
              new Request("url3", System.currentTimeMillis() + 1000)), 1000, TimeUnit.MILLISECONDS).get();
      Assert.assertEquals("boooo", er2.getPayload());
      retryExpect4.assertObservation();
    }
  }

  @Test
  public void testSpecificExceptionRetryPolicy() throws TimeoutException, InterruptedException {
    LogAssert expect = TestLoggers.sys().expect(LOG.getName(), Level.DEBUG, LogMatchers.hasFormat("encontered"));
    RetryPolicy<Object, Callable<Object>> policy =
            RetryPolicy.newBuilder().withExceptionPartialPredicate(IllegalStateException.class,
            (ex, call) -> {
              LOG.debug("encontered", ex);
              return RetryDecision.abort();
            }).build();
    try {
      policy.run(() -> {
        throw new IllegalStateException();
      }, RuntimeException.class);
      Assert.fail();
    } catch (IllegalStateException ex) {
         expect.assertObservation();
    }
  }


}
