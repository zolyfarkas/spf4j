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

/**
 *
 * @author Zoltan Farkas
 */
public final class Request {

  private final String url;

  private final long deadlineMSEpoch;


  public Request(final String url, final long deadlineMSEpoch) {
    this.url = url;
    this.deadlineMSEpoch = deadlineMSEpoch;
  }

  public String getUrl() {
    return url;
  }

  public long getDeadlineMSEpoch() {
    return deadlineMSEpoch;
  }

  @Override
  public String toString() {
    return "Request{" + "url=" + url + ", deadlineMSEpoch=" + deadlineMSEpoch + '}';
  }

}
