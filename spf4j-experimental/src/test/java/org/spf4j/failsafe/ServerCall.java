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

import org.spf4j.base.Timing;

/**
 *
 * @author Zoltan Farkas
 */
public class ServerCall implements TimeoutCallable<Response>{

  private final Server server;

  private final Request request;

  public ServerCall(Server server, Request request) {
    this.server = server;
    this.request = request;
  }

  @Override
  public Response call() throws Exception {
    return server.execute(request);
  }

  public Server getServer() {
    return server;
  }

  public Request getRequest() {
    return request;
  }

  @Override
  public long getDeadlineNanos() {
    return Timing.getCurrentTiming().fromEpochMillisToNanoTime(request.getDeadlineMSEpoch());
  }


}
