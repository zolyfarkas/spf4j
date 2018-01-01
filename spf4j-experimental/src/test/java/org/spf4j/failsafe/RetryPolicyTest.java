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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Throwables;

/**
 *
 * @author Zoltan Farkas
 */
public class RetryPolicyTest {


  @Test(expected = IOException.class)
  public void testNoRetryPolicy() throws IOException, InterruptedException, TimeoutException {
    RetryPolicy<Object, Callable<Object>> rp = RetryPolicy.newBuilder().build((Class) Callable.class);
    rp.execute(() -> { throw new IOException(); }, IOException.class);
  }

  @Test
  public void testNoRetryPolicy2() throws InterruptedException, TimeoutException {
    RetryPolicy<Object, Callable<Object>> rp = RetryPolicy.newBuilder().build((Class) Callable.class);
    String result = (String) rp.execute(() -> "test", RuntimeException.class);
    Assert.assertEquals("test", result);
  }


  @Test
  public void testComplexRetry() throws InterruptedException, TimeoutException, IOException {
    RetryPolicy<Response, ServerCall> rp = RetryPolicy.<Response, ServerCall>newBuilder()
            .exceptionPredicateBuilder()
            .withPartialPredicate((t, sc) ->
              Throwables.getIsRetryablePredicate().test(t) ? RetryDecision.retryDefault(sc) : null).finishPredicate()
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
    Response response1 = new Response(Response.Type.OK, "");
    server.setResponse("url1", response1);
    server.setResponse("url2", new Response(Response.Type.REDIRECT, "url1"));
    Response resp =
            rp.execute(new ServerCall(server, new Request("url2", System.currentTimeMillis() + 10)), IOException.class);
    Assert.assertEquals(response1, resp);
  }


}
