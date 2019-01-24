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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public final class Server {

  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private final Map<String, Function<Request, Response>> responses;

  private volatile Exception breakException;

  public Server() {
    responses = new HashMap<>();
  }

  public void breakException(final Exception ex) {
    LOG.info("Setting break exception", ex);
    breakException = ex;
  }

  public void setResponse(final String url, final Function<Request, Response> response) {
    responses.put(url, response);
  }


  public Response execute(final Request request) throws Exception {
    Exception be = breakException;
    if (be != null) {
      LOG.debug("broken, throwing exception");
      throw be;
    }
    long deadlineMSEpoch = request.getDeadlineMSEpoch();
    long timeout = deadlineMSEpoch - System.currentTimeMillis();
    if (timeout < 0) {
      LOG.debug("Response complete with timeout {}", timeout);
      return new Response(Response.Type.TRANSIENT_ERROR, timeout);
    }
    Response resp = responses.get(request.getUrl()).apply(request);
    if (resp == null) {
      resp = new Response(Response.Type.CLIENT_ERROR, null);
    }
    LOG.debug("Response complete {}", resp);
    return resp;
  }

  @Override
  public String toString() {
    return "Server{" + "responses=" + responses + ", breakException=" + breakException + '}';
  }



}
